package com.alahlia.actionsheet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Document Repository Service — stores documents organized by date
 * on the shared drive at Z:\Action Sheet System\data\repository\
 * 
 * Layout: repository/YYYY/MM/DD/
 *   ├── repo_metadata.json
 *   └── <uploaded files>
 */
@Service
@Slf4j
public class DocumentRepositoryService {

    @Value("${app.repository-path:./data/repository}")
    private String repositoryRoot;

    private final ObjectMapper mapper;
    private static final String METADATA_FILE = "repo_metadata.json";

    public DocumentRepositoryService() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        File root = new File(repositoryRoot);
        if (!root.exists()) {
            root.mkdirs();
            log.info("Created repository root: {}", repositoryRoot);
        }
        log.info("Document Repository initialized at: {}", repositoryRoot);
    }

    // ======================= DATA MODEL =======================

    @Data
    public static class RepoDocument implements Serializable {
        private static final long serialVersionUID = 2L;
        private String id;
        private String fileName;
        private String originalName;
        private String uploaderName;
        private long uploadTimestamp;
        private long fileSize;
        private boolean deleted = false;
        private String deletedBy;
        private long deletedTimestamp;

        public RepoDocument() {}

        public RepoDocument(String fileName, String originalName, String uploaderName, long fileSize) {
            this.id = UUID.randomUUID().toString();
            this.fileName = fileName;
            this.originalName = originalName;
            this.uploaderName = uploaderName;
            this.fileSize = fileSize;
            this.uploadTimestamp = System.currentTimeMillis();
        }
    }

    // ======================= DIRECTORY HELPERS =======================

    private File getDateDirectory(String dateKey) {
        // dateKey: "YYYY-MM-DD"
        String[] parts = dateKey.split("-");
        String path = repositoryRoot
                + File.separator + parts[0]
                + File.separator + parts[1]
                + File.separator + parts[2];
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private String dateToKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    // ======================= CRUD =======================

    /**
     * Upload a document for a specific date.
     */
    public RepoDocument uploadDocument(String dateKey, File sourceFile, String uploaderName) throws IOException {
        File dateDir = getDateDirectory(dateKey);
        String safeName = sanitizeFileName(sourceFile.getName());

        // Avoid collisions
        File destFile = new File(dateDir, safeName);
        if (destFile.exists()) {
            String baseName = safeName;
            String ext = "";
            int dotIdx = safeName.lastIndexOf('.');
            if (dotIdx > 0) {
                baseName = safeName.substring(0, dotIdx);
                ext = safeName.substring(dotIdx);
            }
            safeName = baseName + "_" + System.currentTimeMillis() + ext;
            destFile = new File(dateDir, safeName);
        }

        // Copy file
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Create metadata entry
        RepoDocument doc = new RepoDocument(safeName, sourceFile.getName(), uploaderName, sourceFile.length());

        // Update metadata
        synchronized (this) {
            List<RepoDocument> docs = loadMetadata(dateDir);
            docs.add(doc);
            saveMetadata(dateDir, docs);
        }

        log.info("Repository: Uploaded {} to {} by {}", safeName, dateKey, uploaderName);
        return doc;
    }

    /**
     * Upload from multipart (byte array).
     */
    public RepoDocument uploadDocument(String dateKey, String originalName, byte[] content, String uploaderName) throws IOException {
        File dateDir = getDateDirectory(dateKey);
        String safeName = sanitizeFileName(originalName);

        File destFile = new File(dateDir, safeName);
        if (destFile.exists()) {
            String baseName = safeName;
            String ext = "";
            int dotIdx = safeName.lastIndexOf('.');
            if (dotIdx > 0) {
                baseName = safeName.substring(0, dotIdx);
                ext = safeName.substring(dotIdx);
            }
            safeName = baseName + "_" + System.currentTimeMillis() + ext;
            destFile = new File(dateDir, safeName);
        }

        Files.write(destFile.toPath(), content);

        RepoDocument doc = new RepoDocument(safeName, originalName, uploaderName, content.length);

        synchronized (this) {
            List<RepoDocument> docs = loadMetadata(dateDir);
            docs.add(doc);
            saveMetadata(dateDir, docs);
        }

        log.info("Repository: Uploaded {} to {} by {}", safeName, dateKey, uploaderName);
        return doc;
    }

    /**
     * Get visible (non-deleted) documents for a date.
     */
    public List<RepoDocument> getDocumentsForDate(String dateKey) {
        File dateDir = getDateDirectory(dateKey);
        return loadMetadata(dateDir).stream()
                .filter(d -> !d.isDeleted())
                .collect(Collectors.toList());
    }

    /**
     * Get deleted documents for a date.
     */
    public List<RepoDocument> getDeletedDocumentsForDate(String dateKey) {
        File dateDir = getDateDirectory(dateKey);
        return loadMetadata(dateDir).stream()
                .filter(RepoDocument::isDeleted)
                .collect(Collectors.toList());
    }

    /**
     * Get actual file for download/open.
     */
    public File getDocumentFile(String dateKey, String fileName) {
        File dateDir = getDateDirectory(dateKey);
        return new File(dateDir, fileName);
    }

    /**
     * Soft-delete a document.
     */
    public boolean deleteDocument(String dateKey, String docId, String deletedBy) {
        File dateDir = getDateDirectory(dateKey);
        synchronized (this) {
            List<RepoDocument> docs = loadMetadata(dateDir);
            for (RepoDocument d : docs) {
                if (d.getId().equals(docId)) {
                    d.setDeleted(true);
                    d.setDeletedBy(deletedBy != null ? deletedBy : "Unknown");
                    d.setDeletedTimestamp(System.currentTimeMillis());
                    saveMetadata(dateDir, docs);
                    log.info("Repository: Soft-deleted {} from {} by {}", d.getFileName(), dateKey, deletedBy);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Restore a soft-deleted document.
     */
    public boolean restoreDocument(String dateKey, String docId) {
        File dateDir = getDateDirectory(dateKey);
        synchronized (this) {
            List<RepoDocument> docs = loadMetadata(dateDir);
            for (RepoDocument d : docs) {
                if (d.getId().equals(docId) && d.isDeleted()) {
                    d.setDeleted(false);
                    d.setDeletedBy(null);
                    d.setDeletedTimestamp(0);
                    saveMetadata(dateDir, docs);
                    log.info("Repository: Restored {} on {}", d.getFileName(), dateKey);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get dates with documents for a given month (1-indexed).
     */
    public Set<Integer> getDatesWithDocuments(int year, int month) {
        Set<Integer> days = new HashSet<>();
        String monthPath = repositoryRoot
                + File.separator + String.format("%04d", year)
                + File.separator + String.format("%02d", month);
        File monthDir = new File(monthPath);
        if (monthDir.exists() && monthDir.isDirectory()) {
            File[] dayDirs = monthDir.listFiles(File::isDirectory);
            if (dayDirs != null) {
                for (File dayDir : dayDirs) {
                    try {
                        int day = Integer.parseInt(dayDir.getName());
                        List<RepoDocument> docs = loadMetadata(dayDir);
                        if (docs.stream().anyMatch(d -> !d.isDeleted())) {
                            days.add(day);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return days;
    }

    // ======================= METADATA I/O =======================

    private List<RepoDocument> loadMetadata(File dateDir) {
        File metaFile = new File(dateDir, METADATA_FILE);
        if (!metaFile.exists()) return new ArrayList<>();
        try {
            RepoDocument[] arr = mapper.readValue(metaFile, RepoDocument[].class);
            return new ArrayList<>(Arrays.asList(arr));
        } catch (Exception e) {
            log.error("Repository: Error reading metadata from {}: {}", metaFile.getPath(), e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveMetadata(File dateDir, List<RepoDocument> docs) {
        File metaFile = new File(dateDir, METADATA_FILE);
        try {
            mapper.writeValue(metaFile, docs);
        } catch (Exception e) {
            log.error("Repository: Error writing metadata to {}: {}", metaFile.getPath(), e.getMessage());
        }
    }

    private String sanitizeFileName(String name) {
        String safe = new File(name).getName();
        safe = safe.replaceAll("[^a-zA-Z0-9._\\-() ]", "_");
        if (safe.isEmpty()) safe = "unnamed_file";
        return safe;
    }
}
