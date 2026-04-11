package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.dto.ActionSheetDTO;
import com.alahlia.actionsheet.dto.CreateSheetRequest;
import com.alahlia.actionsheet.dto.DtoMapper;
import com.alahlia.actionsheet.entity.ActionSheet;
import com.alahlia.actionsheet.entity.ConflictEvent;
import com.alahlia.actionsheet.entity.ConflictThread;
import com.alahlia.actionsheet.entity.Employee;
import com.alahlia.actionsheet.exception.ResourceNotFoundException;
import com.alahlia.actionsheet.repository.ActionSheetRepository;
import com.alahlia.actionsheet.repository.EmployeeRepository;
import com.alahlia.actionsheet.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Core business logic for Action Sheets.
 * Ports complete logic from ActionSheetManager.java including:
 * - Status derivation with boss-override conflict resolution
 * - GM lock functionality
 * - Conflict detection (MAJOR vs MINOR)
 * - Priority fallback logic
 * - Duplicate response prevention (2-second window)
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ActionSheetService {

    private final ActionSheetRepository actionSheetRepository;
    private final EmployeeRepository employeeRepository;
    private final WebSocketEventPublisher wsPublisher;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final DraftRecoveryService draftRecoveryService;
    private final AttachmentService attachmentService;

    // Duplicate response prevention — tracks last response timestamp per (sheetId, email)
    private final Map<String, Long> lastResponseTimestamps = new ConcurrentHashMap<>();
    private static final long DUPLICATE_WINDOW_MS = 2000;

    // Status classification
    private static final Set<String> POSITIVE_STATUSES = Set.of(
            "ACTION TAKEN", "COMPLETED", "DONE", "FINISHED", "APPROVED", "ACCEPTED", "NOTED", "ACKNOWLEDGED");

    private static final Set<String> NEGATIVE_STATUSES = Set.of(
            "REJECTED", "DECLINED", "REFUSED", "REJECTED / RETURNED");

    // Initialize attachments directory on startup
    @jakarta.annotation.PostConstruct
    public void init() {
        attachmentService.ensureAttachmentsDirectory();
    }

    // ======================= CRUD =======================

    @Transactional(readOnly = true)
    public List<ActionSheetDTO> getAllActionSheets() {
        return actionSheetRepository.findByDeletedFalseOrderByDueDateAsc()
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActionSheetDTO getActionSheetDto(String id) {
        ActionSheet sheet = actionSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", id));
        return DtoMapper.toDto(sheet);
    }

    /**
     * Internal method that returns the entity (for other services).
     */
    public ActionSheet getActionSheetEntity(String id) {
        return actionSheetRepository.findById(id).orElse(null);
    }

    public ActionSheetDTO createActionSheet(CreateSheetRequest request) {
        ActionSheet sheet = new ActionSheet();
        sheet.setId(generateId(request.getProjectId()));
        sheet.setTitle(request.getTitle().trim());
        sheet.setDueDate(request.getDueDate() != null ? request.getDueDate() : LocalDateTime.now().plusDays(7));
        sheet.setProjectId(request.getProjectId());
        sheet.setAssignedTo(request.getAssignedTo() != null ? request.getAssignedTo() : new HashMap<>());
        sheet.setRecipientTypes(request.getRecipientTypes() != null ? request.getRecipientTypes() : new HashMap<>());
        sheet.setFormData(request.getFormData() != null ? request.getFormData() : new HashMap<>());
        sheet.setCreatedDate(LocalDateTime.now());

        // If status is PENDING, transition directly to IN_PROGRESS (skip DRAFT)
        if ("PENDING".equalsIgnoreCase(request.getStatus())) {
            sheet.setWorkflowState(ActionSheet.WorkflowState.IN_PROGRESS);
            sheet.setStatus("PENDING");
            log.info("Sheet {} created and sent directly (PENDING)", sheet.getId());
        } else {
            sheet.setWorkflowState(ActionSheet.WorkflowState.DRAFT);
            sheet.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        }
        sheet.updateTimestamp();

        ActionSheet saved = actionSheetRepository.save(sheet);

        // If sent directly (PENDING), generate PDF and send emails
        if ("PENDING".equalsIgnoreCase(request.getStatus())) {
            generateAndSendEmails(saved);
            saved = actionSheetRepository.save(saved); // save pdfPath
        }

        // Auto-snapshot for draft recovery (ALL sheets, not just drafts)
        try {
            draftRecoveryService.saveSnapshot(saved);
        } catch (Exception e) {
            log.warn("Draft snapshot failed for {}: {}", saved.getId(), e.getMessage());
        }

        wsPublisher.publishSheetCreated(saved);
        log.info("Created action sheet: {} — {}", saved.getId(), saved.getTitle());
        return DtoMapper.toDto(saved);
    }

    public ActionSheetDTO updateActionSheet(String id, CreateSheetRequest request) {
        ActionSheet existing = actionSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", id));

        existing.setTitle(request.getTitle().trim());
        if (request.getDueDate() != null) existing.setDueDate(request.getDueDate());
        existing.setProjectId(request.getProjectId());
        if (request.getAssignedTo() != null) existing.setAssignedTo(request.getAssignedTo());
        if (request.getRecipientTypes() != null) existing.setRecipientTypes(request.getRecipientTypes());
        if (request.getFormData() != null) existing.setFormData(request.getFormData());

        // Handle status transition: if request says PENDING and sheet is DRAFT,
        // transition workflowState to IN_PROGRESS (i.e. "send" the sheet)
        if (request.getStatus() != null) {
            if ("PENDING".equalsIgnoreCase(request.getStatus()) &&
                    existing.getWorkflowState() == ActionSheet.WorkflowState.DRAFT) {
                existing.setWorkflowState(ActionSheet.WorkflowState.IN_PROGRESS);
                existing.setStatus("PENDING");
                log.info("Sheet {} transitioned from DRAFT to IN_PROGRESS (sent)", id);
                // Generate PDF and send emails
                generateAndSendEmails(existing);
            } else {
                existing.setStatus(request.getStatus());
            }
        }
        existing.updateTimestamp();

        updateSheetStatus(existing);

        ActionSheet saved = actionSheetRepository.save(existing);

        // Auto-snapshot for draft recovery
        try {
            draftRecoveryService.saveSnapshot(saved);
        } catch (Exception e) {
            log.warn("Draft snapshot failed for {}: {}", saved.getId(), e.getMessage());
        }

        wsPublisher.publishSheetUpdated(saved);
        log.info("Updated action sheet: {}", id);
        return DtoMapper.toDto(saved);
    }

    public boolean deleteActionSheet(String id, String deletedBy) {
        ActionSheet sheet = actionSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", id));
        sheet.softDelete(deletedBy);
        actionSheetRepository.save(sheet);
        wsPublisher.publishSheetDeleted(id);
        log.info("Soft deleted action sheet: {} by {}", id, deletedBy);
        return true;
    }

    public boolean restoreActionSheet(String id) {
        ActionSheet sheet = actionSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", id));
        if (!sheet.isDeleted()) {
            throw new IllegalArgumentException("Sheet is not deleted: " + id);
        }
        sheet.restore();
        actionSheetRepository.save(sheet);
        wsPublisher.publishSheetUpdated(sheet);
        log.info("Restored action sheet: {}", id);
        return true;
    }

    public ActionSheetDTO overrideStatus(String id, String status, String gmEmail, String note) {
        ActionSheet sheet = actionSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", id));
        
        sheet.setStatus(status.toUpperCase());
        if ("DRAFT".equalsIgnoreCase(status)) {
            sheet.setWorkflowState(ActionSheet.WorkflowState.DRAFT);
        } else if ("PENDING".equalsIgnoreCase(status) || "PENDING_REVIEW".equalsIgnoreCase(status)) {
            sheet.setWorkflowState(ActionSheet.WorkflowState.PENDING_REVIEW);
        } else if ("IN PROGRESS".equalsIgnoreCase(status)) {
            sheet.setWorkflowState(ActionSheet.WorkflowState.IN_PROGRESS);
        } else {
            sheet.setWorkflowState(ActionSheet.WorkflowState.COMPLETED);
        }
        
        sheet.setOverriddenBy(gmEmail);
        sheet.setOverrideNote(note);
        sheet.updateTimestamp();
        
        // Re-generate PDF to reflect the new status
        try {
            pdfService.generatePdf(sheet);
        } catch (Exception e) {
            log.warn("Failed to generate PDF during GM override for sheet {}", id, e);
        }
        
        ActionSheet saved = actionSheetRepository.save(sheet);
        
        // Auto-snapshot for draft recovery
        try {
            draftRecoveryService.saveSnapshot(saved);
        } catch (Exception e) {
            log.warn("Draft snapshot failed for {}: {}", saved.getId(), e.getMessage());
        }
        
        wsPublisher.publishSheetUpdated(saved);
        log.info("GM {} overridden sheet {} status to {}", gmEmail, id, status);
        return DtoMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ActionSheetDTO> searchActionSheets(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActionSheets();
        }
        return actionSheetRepository.searchByKeyword(keyword.trim())
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActionSheetDTO> getDraftSheets() {
        return actionSheetRepository.findAllDrafts()
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActionSheetDTO> getDeletedSheets() {
        return actionSheetRepository.findByDeletedTrueOrderByDeletedAtDesc()
                .stream().map(DtoMapper::toDto).collect(Collectors.toList());
    }

    // ======================= WORKFLOW =======================

    /**
     * Transition sheet from DRAFT → IN_PROGRESS and notify via WebSocket.
     */
    public ActionSheetDTO sendSheet(String id) {
        ActionSheet sheet = actionSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", id));

        if (sheet.getWorkflowState() != ActionSheet.WorkflowState.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT sheets can be sent. Current state: " + sheet.getWorkflowState());
        }

        sheet.setWorkflowState(ActionSheet.WorkflowState.IN_PROGRESS);
        sheet.setStatus("PENDING");
        sheet.updateTimestamp();

        // Generate PDF and send emails
        generateAndSendEmails(sheet);

        ActionSheet saved = actionSheetRepository.save(sheet);
        wsPublisher.publishSheetUpdated(saved);
        log.info("Sheet sent to recipients: {}", id);
        return DtoMapper.toDto(saved);
    }

    /**
     * Generate PDF attachment and send email notifications to all assigned recipients.
     * Called when a sheet transitions from DRAFT to IN_PROGRESS.
     */
    private void generateAndSendEmails(ActionSheet sheet) {
        try {
            // 1. Generate PDF
            String pdfPath = pdfService.generatePdf(sheet);
            sheet.setPdfPath(pdfPath);
            log.info("PDF generated for sheet {}: {}", sheet.getId(), pdfPath);

            // 2. Build recipient lists
            Map<String, String> assignedTo = sheet.getAssignedTo();
            if (assignedTo == null || assignedTo.isEmpty()) {
                log.warn("No recipients assigned to sheet {}, skipping email", sheet.getId());
                return;
            }

            List<String> recipientEmails = new ArrayList<>(assignedTo.keySet());
            Map<String, String> recipientNames = new HashMap<>(assignedTo);

            // 3. Build attachments list
            List<File> attachments = new ArrayList<>();
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                attachments.add(pdfFile);
            }

            // 4. Send emails asynchronously (don't block the response)
            emailService.sendActionSheetEmail(sheet, recipientEmails, recipientNames, attachments);

        } catch (Exception e) {
            log.error("Failed to generate/send emails for sheet {}: {}", sheet.getId(), e.getMessage(), e);
            // Don't throw — the sheet send should still succeed even if email fails
        }
    }

    /**
     * Resend emails for an existing sheet (e.g., if initial send failed).
     */
    public ActionSheetDTO resendEmails(String id) {
        ActionSheet sheet = actionSheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", id));

        generateAndSendEmails(sheet);
        ActionSheet saved = actionSheetRepository.save(sheet);
        log.info("Resent emails for sheet: {}", id);
        return DtoMapper.toDto(saved);
    }

    /**
     * Add a response to an action sheet and recalculate status.
     * Includes duplicate prevention — rejects responses within 2-second window.
     */
    public ActionSheetDTO addResponse(String sheetId, String email, String response,
                                      String senderUserId, String senderRole, Integer hierarchyLevel) {
        ActionSheet sheet = actionSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", sheetId));

        String normalizedResponse = response.toUpperCase().trim();

        // Duplicate prevention (2-second window)
        String dedupeKey = sheetId + "|" + email.toLowerCase();
        long now = System.currentTimeMillis();
        Long lastTime = lastResponseTimestamps.get(dedupeKey);
        if (lastTime != null && (now - lastTime) < DUPLICATE_WINDOW_MS) {
            log.warn("Duplicate response rejected ({}ms window): {} on {}", DUPLICATE_WINDOW_MS, email, sheetId);
            return DtoMapper.toDto(sheet);
        }
        lastResponseTimestamps.put(dedupeKey, now);

        // Evict old entries periodically to prevent memory leak
        if (lastResponseTimestamps.size() > 10000) {
            long cutoff = now - 60000; // 1 minute
            lastResponseTimestamps.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        String oldStatus = sheet.getStatus();
        sheet.getResponses().put(email, normalizedResponse);
        updateSheetStatus(sheet);
        sheet.updateTimestamp();

        ActionSheet saved = actionSheetRepository.save(sheet);
        wsPublisher.publishResponseAdded(sheetId, email, normalizedResponse);
        if (!Objects.equals(oldStatus, saved.getStatus())) {
            wsPublisher.publishStatusChanged(sheetId, oldStatus, saved.getStatus());
        }
        log.info("Response recorded for {} on sheet {}: {}", email, sheetId, normalizedResponse);
        return DtoMapper.toDto(saved);
    }

    /**
     * GM override: Set status directly and lock it.
     */
    public ActionSheetDTO gmOverrideStatus(String sheetId, String newStatus, String gmEmail, String note) {
        ActionSheet sheet = actionSheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResourceNotFoundException("ActionSheet", sheetId));

        String oldStatus = sheet.getStatus();
        sheet.setOverriddenBy(gmEmail);
        sheet.setOverrideNote(note);
        sheet.setStatus(newStatus);

        ConflictEvent event = new ConflictEvent();
        event.setActionSheet(sheet);
        event.setTimestamp(LocalDateTime.now());
        event.setSeverity("GM_OVERRIDE");
        event.setDescription("GM Override by " + gmEmail + ": status set to " + newStatus + ". Note: " + note);
        event.setResolvedBy(gmEmail);
        sheet.getConflictLog().add(event);
        sheet.updateTimestamp();

        ActionSheet saved = actionSheetRepository.save(sheet);
        wsPublisher.publishStatusChanged(sheetId, oldStatus, newStatus);
        log.info("GM Override by {}: status set to {} for sheet {}", gmEmail, newStatus, sheetId);
        return DtoMapper.toDto(saved);
    }

    // ======================= STATUS DERIVATION =======================

    /**
     * CORE BUSINESS LOGIC: Update sheet status based on responses.
     * Uses cached employee hierarchy to avoid N+1 queries.
     */
    private void updateSheetStatus(ActionSheet sheet) {
        if (sheet == null) return;

        // DRAFT protection
        if (sheet.getWorkflowState() == ActionSheet.WorkflowState.DRAFT) {
            if (!"DRAFT".equals(sheet.getStatus())) sheet.setStatus("DRAFT");
            return;
        }

        // GM lock protection
        if (sheet.getOverriddenBy() != null) return;

        Map<String, String> responses = sheet.getResponses();
        if (responses == null || responses.isEmpty()) {
            handleNoResponses(sheet);
            return;
        }

        // Collect per-user statuses
        Map<String, String> userStatuses = new HashMap<>();
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (entry.getValue() != null) {
                userStatuses.put(entry.getKey(), entry.getValue().toUpperCase().trim());
            }
        }
        sheet.setUserStatuses(userStatuses);

        // Classify and detect conflicts
        boolean hasPositive = false, hasNegative = false;
        for (String r : userStatuses.values()) {
            if (NEGATIVE_STATUSES.contains(r) || r.contains("REJECT")) hasNegative = true;
            if (POSITIVE_STATUSES.contains(r)) hasPositive = true;
        }

        if (hasPositive && hasNegative) {
            sheet.setHasConflict(true);
            sheet.setConflictSeverity("MAJOR");
        } else if (userStatuses.values().stream().distinct().count() > 1) {
            sheet.setHasConflict(true);
            sheet.setConflictSeverity("MINOR");
        } else {
            sheet.setHasConflict(false);
            sheet.setConflictSeverity(null);
        }

        // Boss override — load employee hierarchy ONCE (cached for this call)
        Map<String, Integer> hierarchyCache = buildHierarchyCache();

        String resolvedStatus = null;
        String resolvedBy = null;
        int bestLevel = Integer.MAX_VALUE;
        boolean hasTie = false;

        for (Map.Entry<String, String> entry : userStatuses.entrySet()) {
            int level = hierarchyCache.getOrDefault(entry.getKey().toLowerCase(), 5);
            if (level < bestLevel) {
                bestLevel = level;
                resolvedStatus = entry.getValue();
                resolvedBy = entry.getKey();
                hasTie = false;
            } else if (level == bestLevel && resolvedStatus != null && !resolvedStatus.equals(entry.getValue())) {
                hasTie = true;
            }
        }

        if (hasTie) {
            resolvedStatus = null;
            resolvedBy = null;
        }

        // Log conflict resolution
        if (resolvedStatus != null && sheet.isHasConflict()) {
            ConflictEvent event = new ConflictEvent();
            event.setActionSheet(sheet);
            event.setTimestamp(LocalDateTime.now());
            event.setSeverity(sheet.getConflictSeverity());
            event.setDescription("Conflict resolved by boss override: " + resolvedBy +
                    " (hierarchy level=" + bestLevel + ") -> " + resolvedStatus);
            event.setResolvedBy(resolvedBy);
            sheet.getConflictLog().add(event);
            log.info("Conflict resolved by boss override: {} (level={}) -> {}", resolvedBy, bestLevel, resolvedStatus);
        }

        // Set final status
        if (resolvedStatus != null) {
            String displayStatus = mapToDisplayStatus(resolvedStatus);
            sheet.setStatus(displayStatus);
            // AUTO-COMPLETE: if resolved to ACTION TAKEN, mark workflow as COMPLETED
            if ("ACTION TAKEN".equals(displayStatus)) {
                sheet.setWorkflowState(ActionSheet.WorkflowState.COMPLETED);
                log.info("Sheet {} auto-completed: ACTION TAKEN response received", sheet.getId());
            }
        } else {
            applyPriorityFallback(sheet, userStatuses);
        }
    }

    /**
     * Build a lowercase-email -> hierarchy-level map from all employees.
     * Called once per status update to avoid N+1 queries.
     */
    private Map<String, Integer> buildHierarchyCache() {
        Map<String, Integer> cache = new HashMap<>();
        for (Employee emp : employeeRepository.findAll()) {
            if (emp.getEmail() != null) {
                cache.put(emp.getEmail().toLowerCase(),
                        emp.getHierarchyLevel() != null ? emp.getHierarchyLevel() : 5);
            }
        }
        return cache;
    }

    private void handleNoResponses(ActionSheet sheet) {
        Map<String, String> recipientTypes = sheet.getRecipientTypes();
        Map<String, String> assignedTo = sheet.getAssignedTo();

        if (assignedTo != null && !assignedTo.isEmpty() &&
                recipientTypes != null && !recipientTypes.isEmpty()) {
            boolean hasActionRecipient = assignedTo.keySet().stream()
                    .anyMatch(email -> {
                        String type = recipientTypes.get(email);
                        return type == null || "ACTION".equalsIgnoreCase(type);
                    });
            if (!hasActionRecipient) {
                sheet.setStatus("INFORMATIONAL ONLY");
                return;
            }
        }

        if (sheet.getWorkflowState() == ActionSheet.WorkflowState.PENDING_REVIEW ||
                sheet.getWorkflowState() == ActionSheet.WorkflowState.IN_PROGRESS) {
            if (!"PENDING".equals(sheet.getStatus())) {
                sheet.setStatus("PENDING");
            }
            return;
        }

        if (!"PENDING".equals(sheet.getStatus())) {
            sheet.setStatus("PENDING");
        }
    }

    private String mapToDisplayStatus(String response) {
        if (response == null) return "PENDING";
        String r = response.toUpperCase().trim();

        if (r.contains("REJECT") || "DECLINED".equals(r) || "REFUSED".equals(r)) return "REJECTED / RETURNED";
        if (r.contains("REVIEW")) return "REVIEW REQUESTED";
        if ("IN PROGRESS".equals(r) || "WORKING".equals(r)) return "IN PROGRESS";
        if ("COMPLETED".equals(r) || "DONE".equals(r) || "FINISHED".equals(r) || "ACTION TAKEN".equals(r))
            return "ACTION TAKEN";
        if ("APPROVED".equals(r) || "ACCEPTED".equals(r)) return "APPROVED";
        if ("NOTED".equals(r) || "ACKNOWLEDGED".equals(r)) return "NOTED";
        return "PENDING";
    }

    private void applyPriorityFallback(ActionSheet sheet, Map<String, String> userStatuses) {
        boolean hasRejected = false, hasReview = false, hasActionTaken = false;
        boolean hasNoted = false, hasApproved = false, hasInProgress = false;

        for (String r : userStatuses.values()) {
            if (r.contains("REJECT") || "DECLINED".equals(r) || "REFUSED".equals(r)) hasRejected = true;
            if (r.contains("REVIEW")) hasReview = true;
            if ("COMPLETED".equals(r) || "DONE".equals(r) || "FINISHED".equals(r) || "ACTION TAKEN".equals(r))
                hasActionTaken = true;
            if ("NOTED".equals(r) || "ACKNOWLEDGED".equals(r)) hasNoted = true;
            if ("APPROVED".equals(r) || "ACCEPTED".equals(r)) hasApproved = true;
            if ("IN PROGRESS".equals(r) || "WORKING".equals(r)) hasInProgress = true;
        }

        if (hasRejected) sheet.setStatus("REJECTED / RETURNED");
        else if (hasReview) sheet.setStatus("REVIEW REQUESTED");
        else if (hasInProgress) sheet.setStatus("IN PROGRESS");
        else if (hasActionTaken) {
            sheet.setStatus("ACTION TAKEN");
            sheet.setWorkflowState(ActionSheet.WorkflowState.COMPLETED);
            log.info("Sheet {} auto-completed via fallback: ACTION TAKEN", sheet.getId());
        }
        else if (hasApproved) sheet.setStatus("APPROVED");
        else if (hasNoted) sheet.setStatus("NOTED");
        else sheet.setStatus("PENDING");
    }

    // ======================= HELPERS =======================

    public static boolean isDraftStatus(ActionSheet sheet) {
        if (sheet == null) return false;
        if (sheet.getWorkflowState() == ActionSheet.WorkflowState.DRAFT) return true;
        String st = sheet.getStatus();
        if (st == null || st.trim().isEmpty()) return true;
        return st.contains("DRAFT") || st.contains("WAITING") || st.contains("COMPLETION");
    }

    private String generateId(String projectId) {
        String prefix = "AS";
        if (projectId != null && !projectId.isEmpty()) {
            prefix = projectId.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (prefix.length() > 10) prefix = prefix.substring(0, 10);
        }
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = String.format("%08d", ThreadLocalRandom.current().nextInt(100000000));
        return prefix + "-" + datePart + "-" + random;
    }
}
