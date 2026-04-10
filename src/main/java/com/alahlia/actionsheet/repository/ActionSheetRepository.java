package com.alahlia.actionsheet.repository;

import com.alahlia.actionsheet.entity.ActionSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionSheetRepository extends JpaRepository<ActionSheet, String> {

    // Find all non-deleted sheets
    List<ActionSheet> findByDeletedFalseOrderByDueDateAsc();

    // Find all deleted sheets (for trash view)
    List<ActionSheet> findByDeletedTrueOrderByDeletedAtDesc();

    // Find by project
    List<ActionSheet> findByProjectIdAndDeletedFalse(String projectId);

    // Find by workflow state
    List<ActionSheet> findByWorkflowStateAndDeletedFalse(ActionSheet.WorkflowState state);

    // Find drafts
    @Query("SELECT a FROM ActionSheet a WHERE a.workflowState = 'DRAFT' AND a.deleted = false ORDER BY a.dueDate ASC")
    List<ActionSheet> findAllDrafts();

    // Search by keyword
    @Query("SELECT a FROM ActionSheet a WHERE a.deleted = false AND " +
           "(LOWER(a.id) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.status) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ActionSheet> searchByKeyword(@Param("keyword") String keyword);

    // Find sheets modified after timestamp (for sync)
    List<ActionSheet> findByLastModifiedGreaterThan(Long timestamp);

    // Find overdue sheets
    @Query("SELECT a FROM ActionSheet a WHERE a.deleted = false AND a.dueDate < :now AND " +
           "a.workflowState IN ('IN_PROGRESS', 'PENDING_REVIEW')")
    List<ActionSheet> findOverdueSheets(@Param("now") LocalDateTime now);
}
