package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.dto.ActionSheetDTO;
import com.alahlia.actionsheet.dto.CreateSheetRequest;
import com.alahlia.actionsheet.dto.RespondRequest;
import com.alahlia.actionsheet.service.ActionSheetService;
import com.alahlia.actionsheet.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * REST API for Action Sheets.
 * All endpoints return ActionSheetDTO — never raw JPA entities.
 */
@RestController
@RequestMapping("/api/sheets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Action Sheets", description = "CRUD and workflow operations for action sheets")
public class ActionSheetController {

    private final ActionSheetService actionSheetService;
    private final EmailService emailService;

    @Value("${app.files-path:E:/Action Sheet System/data/files}")
    private String filesPath;

    @GetMapping
    @Operation(summary = "List all action sheets", description = "Returns non-deleted sheets, optionally filtered by search keyword")
    public List<ActionSheetDTO> getAllSheets(@RequestParam(required = false) String search) {
        if (search != null && !search.isEmpty()) {
            return actionSheetService.searchActionSheets(search);
        }
        return actionSheetService.getAllActionSheets();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single action sheet by ID")
    public ActionSheetDTO getSheet(@PathVariable String id) {
        return actionSheetService.getActionSheetDto(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new action sheet (starts as DRAFT)")
    public ActionSheetDTO createSheet(@Valid @RequestBody CreateSheetRequest request) {
        return actionSheetService.createActionSheet(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing action sheet")
    public ActionSheetDTO updateSheet(@PathVariable String id, @Valid @RequestBody CreateSheetRequest request) {
        return actionSheetService.updateActionSheet(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an action sheet")
    public ResponseEntity<Void> deleteSheet(
            @PathVariable String id,
            @RequestParam(defaultValue = "system") String deletedBy) {
        actionSheetService.deleteActionSheet(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted action sheet")
    public ResponseEntity<Void> restoreSheet(@PathVariable String id) {
        actionSheetService.restoreActionSheet(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/drafts")
    @Operation(summary = "List all draft action sheets")
    public List<ActionSheetDTO> getDrafts() {
        return actionSheetService.getDraftSheets();
    }

    @GetMapping("/deleted")
    @Operation(summary = "List all deleted (trashed) action sheets")
    public List<ActionSheetDTO> getDeleted() {
        return actionSheetService.getDeletedSheets();
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send a DRAFT sheet to recipients (DRAFT → IN_PROGRESS)")
    public ActionSheetDTO sendSheet(@PathVariable String id) {
        return actionSheetService.sendSheet(id);
    }

    @PostMapping("/{id}/respond")
    @Operation(summary = "Record a response from a recipient")
    public ActionSheetDTO addResponse(@PathVariable String id, @Valid @RequestBody RespondRequest request) {
        return actionSheetService.addResponse(
                id,
                request.getEmail(),
                request.getResponse(),
                request.getSenderUserId(),
                request.getSenderRole(),
                request.getHierarchyLevel() != null ? request.getHierarchyLevel() : 5
        );
    }

    @PostMapping("/{id}/override")
    @Operation(summary = "GM status override — locks the sheet status")
    public ActionSheetDTO gmOverride(@PathVariable String id, @RequestBody Map<String, String> request) {
        String status = request.get("status");
        String gmEmail = request.get("gmEmail");
        String note = request.get("note");

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required for GM override");
        }
        if (gmEmail == null || gmEmail.isBlank()) {
            throw new IllegalArgumentException("GM email is required");
        }

        return actionSheetService.gmOverrideStatus(id, status, gmEmail, note != null ? note : "");
    }

    @PostMapping("/email/test")
    @Operation(summary = "Send a test email to verify Brevo SMTP configuration")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestBody Map<String, String> request) {
        String toEmail = request.get("email");
        if (toEmail == null || toEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email address required"));
        }
        try {
            emailService.sendTestEmail(toEmail);
            return ResponseEntity.ok(Map.of("message", "Test email sent to " + toEmail));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }



    @PostMapping("/{id}/resend")
    @Operation(summary = "Resend emails for an existing action sheet")
    public ActionSheetDTO resendSheet(@PathVariable String id) {
        return actionSheetService.resendEmails(id);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Serve the PDF for a specific action sheet")
    public ResponseEntity<Resource> servePdf(@PathVariable String id) {
        ActionSheetDTO sheet = actionSheetService.getActionSheetDto(id);
        if (sheet.getPdfPath() == null || sheet.getPdfPath().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(sheet.getPdfPath());
        if (!file.exists() || !file.isFile()) {
            // Try looking in the files directory with just the filename
            String fileName = file.getName();
            file = new File(filesPath, fileName);
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
        }

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ActionSheet_" + id + ".pdf\"")
                .body(resource);
    }

    @GetMapping("/files/{fileName:.+}")
    @Operation(summary = "Serve an attached document file for preview/download")
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        // Try the files directory first
        File file = new File(filesPath, fileName);
        
        // If not found, try looking in project-specific ActionSheets folders
        if (!file.exists() || !file.isFile()) {
            // Extract sheet ID from filename if it follows the pattern ActionSheet_XXX.pdf
            if (fileName.startsWith("ActionSheet_") && fileName.endsWith(".pdf")) {
                String sheetId = fileName.substring(12, fileName.length() - 4);
                try {
                    ActionSheetDTO sheet = actionSheetService.getActionSheetDto(sheetId);
                    if (sheet.getPdfPath() != null) {
                        file = new File(sheet.getPdfPath());
                    }
                } catch (Exception e) {
                    log.debug("Could not find sheet for PDF: {}", fileName);
                }
            }
        }
        
        if (!file.exists() || !file.isFile()) {
            log.warn("File not found: {} (looked in: {})", fileName, filesPath);
            return ResponseEntity.notFound().build();
        }

        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        if (contentType == null) contentType = "application/octet-stream";
        
        // Use inline for PDFs to enable preview
        String disposition = contentType.equals("application/pdf") 
            ? "inline; filename=\"" + file.getName() + "\""
            : "attachment; filename=\"" + file.getName() + "\"";

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }
}
