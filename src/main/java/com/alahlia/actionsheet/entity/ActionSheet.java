package com.alahlia.actionsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;

/**
 * JPA Entity for Action Sheet — migrated from ActionSheetManager.ActionSheet.
 * Uses @Getter/@Setter instead of @Data to avoid Lombok hashCode/equals
 * circular crash with bidirectional JPA relationships.
 */
@Entity
@Table(name = "action_sheets", indexes = {
    @Index(name = "idx_sheet_status", columnList = "status"),
    @Index(name = "idx_sheet_workflow", columnList = "workflowState"),
    @Index(name = "idx_sheet_due_date", columnList = "dueDate"),
    @Index(name = "idx_sheet_deleted", columnList = "deleted"),
    @Index(name = "idx_sheet_project", columnList = "projectId")
})
@Getter
@Setter
@NoArgsConstructor
public class ActionSheet {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(length = 100)
    private String status;

    @Column(length = 500)
    private String pdfPath;

    @Column(length = 50)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WorkflowState workflowState = WorkflowState.DRAFT;

    // Soft delete
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @Column(length = 100)
    private String deletedBy;

    // Sync timestamp
    @Column(nullable = false)
    private Long lastModified = System.currentTimeMillis();

    // Conflict resolution
    @Column(nullable = false)
    private boolean hasConflict = false;

    @Column(length = 20)
    private String conflictSeverity;

    @Column(length = 100)
    private String overriddenBy;

    @Column(length = 1000)
    private String overrideNote;

    // JSON-stored maps (PostgreSQL JSONB / H2 JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, String> assignedTo = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, String> responses = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, String> othersEmails = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> formData = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, String> recipientTypes = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, String> userStatuses = new HashMap<>();

    // Attachments - list of uploaded document filenames
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private List<String> attachments = new ArrayList<>();

    // Relationships — @JsonIgnore prevents infinite recursion during serialization.
    // These are exposed through DTOs via DtoMapper instead.
    @JsonIgnore
    @OneToMany(mappedBy = "actionSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResponseEntry> responseHistory = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "actionSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConflictEvent> conflictLog = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "actionSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConflictThread> conflictThreads = new ArrayList<>();

    // Workflow states
    public enum WorkflowState {
        DRAFT,
        PENDING_REVIEW,
        IN_PROGRESS,
        COMPLETED
    }

    // Soft delete
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.lastModified = System.currentTimeMillis();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
        this.lastModified = System.currentTimeMillis();
    }

    public void updateTimestamp() {
        this.lastModified = System.currentTimeMillis();
    }

    // equals/hashCode based on ID only — safe with JPA proxies
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionSheet that = (ActionSheet) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
