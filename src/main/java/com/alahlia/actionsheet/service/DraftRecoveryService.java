package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.entity.ActionSheet;
import com.alahlia.actionsheet.repository.ActionSheetRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Draft Recovery Service — saves complete draft snapshots to the network drive.
 * When a user saves a draft, a full JSON snapshot (including attachments paths)
 * is written to Z:\Action Sheet System\data\draft_recovery\.
 * Admin users can browse and restore these snapshots.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DraftRecoveryService {

    private final ActionSheetRepository actionSheetRepository;

    @Value("${files.path:Z:/Action Sheet System/data}")
    private String dataPath;

    private File recoveryDir;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    @PostConstruct
    public void init() {
        recoveryDir = new File(dataPath, "draft_recovery");
        if (!recoveryDir.exists()) {
            recoveryDir.mkdirs();
            log.info("DraftRecovery: Created recovery directory: {}", recoveryDir.getAbsolutePath());
        }
        log.info("DraftRecovery: Snapshots stored at: {}", recoveryDir.getAbsolutePath());
    }

    /**
     * Save a snapshot of the given draft sheet to the network drive.
     * Called automatically when a user saves a draft.
     */
    public void saveSnapshot(ActionSheet sheet) {
        if (sheet == null || sheet.getId() == null) return;

        try {
            DraftSnapshot snapshot = new DraftSnapshot();
            snapshot.setSheetId(sheet.getId());
            snapshot.setTitle(sheet.getTitle());
            snapshot.setStatus(sheet.getStatus());
            snapshot.setCreatedDate(sheet.getCreatedDate());
            snapshot.setDueDate(sheet.getDueDate());
            snapshot.setProjectId(sheet.getProjectId());
            snapshot.setAssignedTo(sheet.getAssignedTo());
            snapshot.setRecipientTypes(sheet.getRecipientTypes());
            snapshot.setFormData(sheet.getFormData());
            snapshot.setResponses(sheet.getResponses());
            snapshot.setPdfPath(sheet.getPdfPath());
            snapshot.setWorkflowState(sheet.getWorkflowState() != null ? sheet.getWorkflowState().name() : "DRAFT");
            snapshot.setSnapshotTimestamp(LocalDateTime.now());
            snapshot.setSnapshotBy("system");

            // Also capture attachment filenames from formData if present
            if (sheet.getFormData() != null) {
                Object attachments = sheet.getFormData().get("attachments");
                if (attachments instanceof List) {
                    snapshot.setAttachmentFiles((List<String>) attachments);
                }
                Object legacyAttachments = sheet.getFormData().get("legacyAttachments");
                if (legacyAttachments instanceof List) {
                    snapshot.setLegacyAttachmentFiles((List<String>) legacyAttachments);
                }
            }

            // Write snapshot to file: draft_recovery/{sheetId}_{timestamp}.json
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = sheet.getId() + "_" + timestamp + ".json";
            File snapshotFile = new File(recoveryDir, filename);

            mapper.writeValue(snapshotFile, snapshot);
            log.info("DraftRecovery: Saved snapshot for {} -> {}", sheet.getId(), snapshotFile.getName());

        } catch (Exception e) {
            log.error("DraftRecovery: Failed to save snapshot for {}: {}", sheet.getId(), e.getMessage());
        }
    }

    /**
     * List all available snapshots (sorted by newest first).
     */
    public List<DraftSnapshot> listSnapshots() {
        List<DraftSnapshot> snapshots = new ArrayList<>();

        File[] files = recoveryDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return snapshots;

        for (File file : files) {
            try {
                DraftSnapshot snapshot = mapper.readValue(file, DraftSnapshot.class);
                snapshot.setFileName(file.getName());
                snapshots.add(snapshot);
            } catch (Exception e) {
                log.warn("DraftRecovery: Could not read snapshot {}: {}", file.getName(), e.getMessage());
            }
        }

        // Sort by newest first
        snapshots.sort((a, b) -> {
            if (a.getSnapshotTimestamp() == null) return 1;
            if (b.getSnapshotTimestamp() == null) return -1;
            return b.getSnapshotTimestamp().compareTo(a.getSnapshotTimestamp());
        });

        return snapshots;
    }

    /**
     * Restore a draft sheet from a snapshot file.
     * Creates a new ActionSheet entity in the database from the snapshot data.
     */
    public ActionSheet restoreFromSnapshot(String fileName) throws IOException {
        File snapshotFile = new File(recoveryDir, fileName);
        if (!snapshotFile.exists()) {
            throw new IOException("Snapshot file not found: " + fileName);
        }

        DraftSnapshot snapshot = mapper.readValue(snapshotFile, DraftSnapshot.class);

        // Check if the sheet already exists (not deleted) — prevent duplicate restore
        Optional<ActionSheet> existingOpt = actionSheetRepository.findById(snapshot.getSheetId());
        if (existingOpt.isPresent() && !existingOpt.get().isDeleted()) {
            // Sheet still exists — just update it
            ActionSheet existing = existingOpt.get();
            existing.setTitle(snapshot.getTitle());
            existing.setFormData(snapshot.getFormData());
            existing.setAssignedTo(snapshot.getAssignedTo());
            existing.setRecipientTypes(snapshot.getRecipientTypes());
            existing.setResponses(snapshot.getResponses());
            existing.setPdfPath(snapshot.getPdfPath());
            existing.setWorkflowState(ActionSheet.WorkflowState.DRAFT);
            existing.setStatus("DRAFT");
            existing.setDeleted(false);
            existing.updateTimestamp();
            ActionSheet saved = actionSheetRepository.save(existing);
            log.info("DraftRecovery: Restored (updated existing) sheet {}", saved.getId());
            return saved;
        } else if (existingOpt.isPresent() && existingOpt.get().isDeleted()) {
            // Sheet was soft-deleted — restore it
            ActionSheet existing = existingOpt.get();
            existing.setTitle(snapshot.getTitle());
            existing.setFormData(snapshot.getFormData());
            existing.setAssignedTo(snapshot.getAssignedTo());
            existing.setRecipientTypes(snapshot.getRecipientTypes());
            existing.setResponses(snapshot.getResponses());
            existing.setPdfPath(snapshot.getPdfPath());
            existing.setWorkflowState(ActionSheet.WorkflowState.DRAFT);
            existing.setStatus("DRAFT");
            existing.restore();
            existing.updateTimestamp();
            ActionSheet saved = actionSheetRepository.save(existing);
            log.info("DraftRecovery: Restored (un-deleted) sheet {}", saved.getId());
            return saved;
        } else {
            // Sheet doesn't exist at all — create new
            ActionSheet sheet = new ActionSheet();
            sheet.setId(snapshot.getSheetId());
            sheet.setTitle(snapshot.getTitle());
            sheet.setCreatedDate(snapshot.getCreatedDate() != null ? snapshot.getCreatedDate() : LocalDateTime.now());
            sheet.setDueDate(snapshot.getDueDate());
            sheet.setProjectId(snapshot.getProjectId());
            sheet.setFormData(snapshot.getFormData());
            sheet.setAssignedTo(snapshot.getAssignedTo() != null ? snapshot.getAssignedTo() : new HashMap<>());
            sheet.setRecipientTypes(snapshot.getRecipientTypes() != null ? snapshot.getRecipientTypes() : new HashMap<>());
            sheet.setResponses(snapshot.getResponses() != null ? snapshot.getResponses() : new HashMap<>());
            sheet.setPdfPath(snapshot.getPdfPath());
            sheet.setWorkflowState(ActionSheet.WorkflowState.DRAFT);
            sheet.setStatus("DRAFT");
            sheet.updateTimestamp();
            ActionSheet saved = actionSheetRepository.save(sheet);
            log.info("DraftRecovery: Restored (created new) sheet {}", saved.getId());
            return saved;
        }
    }

    /**
     * Delete a specific snapshot file.
     */
    public boolean deleteSnapshot(String fileName) {
        File file = new File(recoveryDir, fileName);
        if (file.exists() && file.delete()) {
            log.info("DraftRecovery: Deleted snapshot {}", fileName);
            return true;
        }
        return false;
    }

    // ── Snapshot data class ──

    @Data
    public static class DraftSnapshot {
        private String sheetId;
        private String title;
        private String status;
        private String workflowState;
        private LocalDateTime createdDate;
        private LocalDateTime dueDate;
        private String projectId;
        private Map<String, String> assignedTo;
        private Map<String, String> recipientTypes;
        private Map<String, Object> formData;
        private Map<String, String> responses;
        private String pdfPath;
        private LocalDateTime snapshotTimestamp;
        private String snapshotBy;
        private List<String> attachmentFiles;
        private List<String> legacyAttachmentFiles;
        // Not persisted — set after reading from disk
        private transient String fileName;
    }
}
