package com.alahlia.actionsheet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing action sheet attachments.
 * Handles file upload, storage, retrieval, and deletion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private static final String ATTACHMENTS_DIR = "data/attachments";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    /**
     * Upload multiple files for an action sheet.
     * Files are stored in data/attachments/{sheetId}/
     */
    public List<String> uploadAttachments(String sheetId, MultipartFile[] files) throws IOException {
        List<String> savedFileNames = new ArrayList<>();
        
        if (files == null || files.length == 0) {
            return savedFileNames;
        }

        // Create directory for this sheet's attachments
        Path sheetDir = Paths.get(ATTACHMENTS_DIR, sheetId);
        Files.createDirectories(sheetDir);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                log.warn("File {} exceeds max size ({}MB), skipping", file.getOriginalFilename(), MAX_FILE_SIZE / 1024 / 1024);
                continue;
            }

            // Generate safe filename: UUID_originalName
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "unnamed_file";
            }
            
            // Sanitize filename
            String safeFilename = sanitizeFilename(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + safeFilename;
            
            Path targetPath = sheetDir.resolve(uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            savedFileNames.add(uniqueFilename);
            log.info("Saved attachment: {} for sheet {}", uniqueFilename, sheetId);
        }

        return savedFileNames;
    }

    /**
     * Get the file path for an attachment.
     */
    public File getAttachmentFile(String sheetId, String fileName) {
        Path filePath = Paths.get(ATTACHMENTS_DIR, sheetId, fileName);
        File file = filePath.toFile();
        
        if (!file.exists()) {
            log.warn("Attachment not found: {}/{}", sheetId, fileName);
            return null;
        }
        
        return file;
    }

    /**
     * Delete all attachments for a sheet.
     */
    public void deleteSheetAttachments(String sheetId) {
        Path sheetDir = Paths.get(ATTACHMENTS_DIR, sheetId);
        
        if (!Files.exists(sheetDir)) {
            return;
        }

        try {
            Files.walk(sheetDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete: {}", path, e);
                    }
                });
            log.info("Deleted all attachments for sheet: {}", sheetId);
        } catch (IOException e) {
            log.error("Failed to delete attachments for sheet: {}", sheetId, e);
        }
    }

    /**
     * Delete a specific attachment.
     */
    public boolean deleteAttachment(String sheetId, String fileName) {
        Path filePath = Paths.get(ATTACHMENTS_DIR, sheetId, fileName);
        
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Deleted attachment: {}/{}", sheetId, fileName);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete attachment: {}/{}", sheetId, fileName, e);
            return false;
        }
    }

    /**
     * List all attachments for a sheet.
     */
    public List<String> listAttachments(String sheetId) {
        List<String> attachments = new ArrayList<>();
        Path sheetDir = Paths.get(ATTACHMENTS_DIR, sheetId);
        
        if (!Files.exists(sheetDir)) {
            return attachments;
        }

        try {
            Files.list(sheetDir)
                .filter(Files::isRegularFile)
                .forEach(path -> attachments.add(path.getFileName().toString()));
        } catch (IOException e) {
            log.error("Failed to list attachments for sheet: {}", sheetId, e);
        }

        return attachments;
    }

    /**
     * Sanitize filename to prevent directory traversal and other issues.
     */
    private String sanitizeFilename(String filename) {
        // Remove path separators and other dangerous characters
        String safe = filename.replaceAll("[/\\\\:*?\"<>|]", "_");
        
        // Limit length
        if (safe.length() > 200) {
            String extension = "";
            int dotIndex = safe.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = safe.substring(dotIndex);
                safe = safe.substring(0, Math.min(200 - extension.length(), dotIndex)) + extension;
            } else {
                safe = safe.substring(0, 200);
            }
        }
        
        return safe;
    }

    /**
     * Initialize attachments directory on startup.
     */
    public void ensureAttachmentsDirectory() {
        try {
            Path attachmentsPath = Paths.get(ATTACHMENTS_DIR);
            Files.createDirectories(attachmentsPath);
            log.info("Attachments directory ready: {}", attachmentsPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create attachments directory", e);
        }
    }
}
