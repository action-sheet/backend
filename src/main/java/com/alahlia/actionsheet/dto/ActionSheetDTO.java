package com.alahlia.actionsheet.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flat DTO for ActionSheet API responses.
 * No JPA annotations, no circular references, no lazy-load traps.
 */
@Data
public class ActionSheetDTO {

    private String id;
    private String title;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime dueDate;
    private String workflowState;
    private String projectId;
    private String pdfPath;

    // Soft delete
    private boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedBy;

    // Sync
    private Long lastModified;

    // Conflict
    private boolean hasConflict;
    private String conflictSeverity;
    private String overriddenBy;
    private String overrideNote;

    // Maps
    private Map<String, String> assignedTo = new HashMap<>();
    private Map<String, String> responses = new HashMap<>();
    private Map<String, String> othersEmails = new HashMap<>();
    private Map<String, Object> formData = new HashMap<>();
    private Map<String, String> recipientTypes = new HashMap<>();
    private Map<String, String> userStatuses = new HashMap<>();

    // Flattened collections (not lazy-loaded JPA references)
    private List<ResponseEntryDTO> responseHistory = new ArrayList<>();
    private List<ConflictEventDTO> conflictLog = new ArrayList<>();
    private List<ConflictThreadDTO> conflictThreads = new ArrayList<>();

    // Computed
    private int recipientCount;
    private int responseCount;

    @Data
    public static class ResponseEntryDTO {
        private Long id;
        private String email;
        private String response;
        private LocalDateTime timestamp;
        private boolean overwritten;
        private String senderUserId;
        private String senderRole;
        private Integer senderHierarchyLevel;
    }

    @Data
    public static class ConflictEventDTO {
        private Long id;
        private LocalDateTime timestamp;
        private String severity;
        private String description;
        private String resolvedBy;
        private LocalDateTime resolvedAt;
    }

    @Data
    public static class ConflictThreadDTO {
        private Long id;
        private LocalDateTime timestamp;
        private String author;
        private String message;
    }
}
