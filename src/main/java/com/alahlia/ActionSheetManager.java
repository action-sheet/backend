package com.alahlia;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy ActionSheetManager stub — matches the serialized field layout.
 * Only used for deserialization during migration.
 *
 * IMPORTANT: The actionSheets.dat file contains a Map<String, ActionSheet>
 * directly (not the ActionSheetManager object), but the ActionSheet inner
 * class still resolves as com.alahlia.ActionSheetManager$ActionSheet.
 */
public class ActionSheetManager implements Serializable {
    private static final long serialVersionUID = 1L;

    public Map<String, ActionSheet> actionSheets;
    public long lastLoadedModTime;
    public long lastCheckTime;

    public static class ActionSheet implements Serializable {
        private static final long serialVersionUID = 4L;

        public enum WorkflowState {
            DRAFT,
            PENDING_REVIEW,
            IN_PROGRESS,
            COMPLETED
        }

        public String id;
        public String title;
        public Date createdDate;
        public Date dueDate;
        public String status;
        public Map<String, String> assignedTo;
        public Map<String, String> responses;
        public String pdfPath;
        public Map<String, String> othersEmails;
        public Map<String, Object> formData;
        public Map<String, List<ResponseEntry>> responseHistory;
        public String projectId;
        public Map<String, String> recipientTypes;

        // Soft delete
        public boolean isDeleted;
        public Date deletedAt;
        public String deletedBy;

        // Sync
        public long lastModified;

        // Workflow
        public WorkflowState workflowState;

        // Conflict resolution
        public Map<String, String> userStatuses;
        public boolean hasConflict;
        public String conflictSeverity;
        public List<ConflictEvent> conflictLog;
        public String overriddenBy;
        public String overrideNote;
        public List<ConflictThread> conflictThreads;
    }

    public static class ResponseEntry implements Serializable {
        private static final long serialVersionUID = 2L;

        public String email;
        public String response;
        public Date timestamp;
        public boolean isOverwritten;
        public String rawContent;
        public String senderUserId;
        public String senderRole;
        public int senderHierarchyLevel;
    }
}
