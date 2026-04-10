package com.alahlia.actionsheet.dto;

import com.alahlia.actionsheet.entity.ActionSheet;
import com.alahlia.actionsheet.entity.ConflictEvent;
import com.alahlia.actionsheet.entity.ConflictThread;
import com.alahlia.actionsheet.entity.ResponseEntry;

/**
 * Maps JPA entities to flat DTOs. Breaks all circular references.
 */
public final class DtoMapper {

    private DtoMapper() {}

    public static ActionSheetDTO toDto(ActionSheet entity) {
        if (entity == null) return null;

        ActionSheetDTO dto = new ActionSheetDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setStatus(entity.getStatus());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setDueDate(entity.getDueDate());
        dto.setWorkflowState(entity.getWorkflowState() != null ? entity.getWorkflowState().name() : null);
        dto.setProjectId(entity.getProjectId());
        dto.setPdfPath(entity.getPdfPath());
        dto.setDeleted(entity.isDeleted());
        dto.setDeletedAt(entity.getDeletedAt());
        dto.setDeletedBy(entity.getDeletedBy());
        dto.setLastModified(entity.getLastModified());
        dto.setHasConflict(entity.isHasConflict());
        dto.setConflictSeverity(entity.getConflictSeverity());
        dto.setOverriddenBy(entity.getOverriddenBy());
        dto.setOverrideNote(entity.getOverrideNote());

        // Copy maps (defensive copies)
        if (entity.getAssignedTo() != null) dto.setAssignedTo(entity.getAssignedTo());
        if (entity.getResponses() != null) dto.setResponses(entity.getResponses());
        if (entity.getOthersEmails() != null) dto.setOthersEmails(entity.getOthersEmails());
        if (entity.getFormData() != null) dto.setFormData(entity.getFormData());
        if (entity.getRecipientTypes() != null) dto.setRecipientTypes(entity.getRecipientTypes());
        if (entity.getUserStatuses() != null) dto.setUserStatuses(entity.getUserStatuses());

        // Computed fields
        dto.setRecipientCount(entity.getAssignedTo() != null ? entity.getAssignedTo().size() : 0);
        dto.setResponseCount(entity.getResponses() != null ? entity.getResponses().size() : 0);

        // Collections — only map if initialized (avoid LazyInitializationException)
        try {
            if (entity.getResponseHistory() != null) {
                entity.getResponseHistory().forEach(e -> dto.getResponseHistory().add(toDto(e)));
            }
        } catch (Exception ignored) {}

        try {
            if (entity.getConflictLog() != null) {
                entity.getConflictLog().forEach(e -> dto.getConflictLog().add(toDto(e)));
            }
        } catch (Exception ignored) {}

        try {
            if (entity.getConflictThreads() != null) {
                entity.getConflictThreads().forEach(e -> dto.getConflictThreads().add(toDto(e)));
            }
        } catch (Exception ignored) {}

        return dto;
    }

    public static ActionSheetDTO.ResponseEntryDTO toDto(ResponseEntry entity) {
        ActionSheetDTO.ResponseEntryDTO dto = new ActionSheetDTO.ResponseEntryDTO();
        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setResponse(entity.getResponse());
        dto.setTimestamp(entity.getTimestamp());
        dto.setOverwritten(entity.isOverwritten());
        dto.setSenderUserId(entity.getSenderUserId());
        dto.setSenderRole(entity.getSenderRole());
        dto.setSenderHierarchyLevel(entity.getSenderHierarchyLevel());
        return dto;
    }

    public static ActionSheetDTO.ConflictEventDTO toDto(ConflictEvent entity) {
        ActionSheetDTO.ConflictEventDTO dto = new ActionSheetDTO.ConflictEventDTO();
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getTimestamp());
        dto.setSeverity(entity.getSeverity());
        dto.setDescription(entity.getDescription());
        dto.setResolvedBy(entity.getResolvedBy());
        dto.setResolvedAt(entity.getResolvedAt());
        return dto;
    }

    public static ActionSheetDTO.ConflictThreadDTO toDto(ConflictThread entity) {
        ActionSheetDTO.ConflictThreadDTO dto = new ActionSheetDTO.ConflictThreadDTO();
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getTimestamp());
        dto.setAuthor(entity.getAuthor());
        dto.setMessage(entity.getMessage());
        return dto;
    }
}
