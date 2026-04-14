package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing action sheet attachments.
 */
@RestController
@RequestMapping("/api/sheets/{sheetId}/attachments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final com.alahlia.actionsheet.service.ActionSheetService actionSheetService;

    /**
     * Upload attachments for an action sheet.
     * POST /api/sheets/{sheetId}/attachments
     */
    @PostMapping
    public ResponseEntity<?> uploadAttachments(
            @PathVariable String sheetId,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            List<String> savedFiles = attachmentService.uploadAttachments(sheetId, files);
            
            // Update the action sheet entity with attachment list
            com.alahlia.actionsheet.entity.ActionSheet sheet = actionSheetService.getActionSheetEntity(sheetId);
            if (sheet != null) {
                if (sheet.getAttachments() == null) {
                    sheet.setAttachments(new java.util.ArrayList<>());
                }
                sheet.getAttachments().addAll(savedFiles);
                
                // Check if this sheet is waiting to send email after attachments
                boolean shouldSendEmail = false;
                if (sheet.getFormData() != null && "true".equals(sheet.getFormData().get("_pendingEmailSend"))) {
                    shouldSendEmail = true;
                    sheet.getFormData().remove("_pendingEmailSend"); // Clear the flag
                }
                
                actionSheetService.saveEntity(sheet);
                
                // If sheet was waiting for attachments, send email now
                if (shouldSendEmail) {
                    log.info("Attachments uploaded for sheet {} - triggering email send", sheetId);
                    actionSheetService.sendSheetEmails(sheetId);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("sheetId", sheetId);
            response.put("uploadedFiles", savedFiles);
            response.put("count", savedFiles.size());
            
            log.info("Uploaded {} attachments for sheet {}", savedFiles.size(), sheetId);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Failed to upload attachments for sheet {}", sheetId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload attachments: " + e.getMessage()));
        }
    }

    /**
     * List all attachments for an action sheet.
     * GET /api/sheets/{sheetId}/attachments
     */
    @GetMapping
    public ResponseEntity<?> listAttachments(@PathVariable String sheetId) {
        try {
            List<String> attachments = attachmentService.listAttachments(sheetId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sheetId", sheetId);
            response.put("attachments", attachments);
            response.put("count", attachments.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to list attachments for sheet {}", sheetId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list attachments: " + e.getMessage()));
        }
    }

    /**
     * Download a specific attachment.
     * GET /api/sheets/{sheetId}/attachments/{fileName}
     */
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable String sheetId,
            @PathVariable String fileName) {
        
        File file = attachmentService.getAttachmentFile(sheetId, fileName);
        
        if (file == null || !file.exists()) {
            log.warn("Attachment not found: {}/{}", sheetId, fileName);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        
        // Extract original filename (remove UUID prefix)
        String displayName = fileName;
        if (fileName.contains("_")) {
            displayName = fileName.substring(fileName.indexOf("_") + 1);
        }

        // Determine content type
        String contentType = "application/octet-stream";
        String lowerName = displayName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerName.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            contentType = "application/msword";
        } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            contentType = "application/vnd.ms-excel";
        }

        // Use "inline" for PDFs and images to enable preview, "attachment" for others
        String disposition = (contentType.startsWith("image/") || contentType.equals("application/pdf"))
                ? "inline; filename=\"" + displayName + "\""
                : "attachment; filename=\"" + displayName + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.length())
                .body(resource);
    }

    /**
     * Delete a specific attachment.
     * DELETE /api/sheets/{sheetId}/attachments/{fileName}
     */
    @DeleteMapping("/{fileName}")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable String sheetId,
            @PathVariable String fileName) {
        
        boolean deleted = attachmentService.deleteAttachment(sheetId, fileName);
        
        if (deleted) {
            // Remove from action sheet entity
            com.alahlia.actionsheet.entity.ActionSheet sheet = actionSheetService.getActionSheetEntity(sheetId);
            if (sheet != null && sheet.getAttachments() != null) {
                sheet.getAttachments().remove(fileName);
                actionSheetService.saveEntity(sheet);
            }
            
            return ResponseEntity.ok(Map.of("message", "Attachment deleted", "fileName", fileName));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Attachment not found"));
        }
    }

    /**
     * Delete all attachments for a sheet.
     * DELETE /api/sheets/{sheetId}/attachments
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAllAttachments(@PathVariable String sheetId) {
        attachmentService.deleteSheetAttachments(sheetId);
        return ResponseEntity.ok(Map.of("message", "All attachments deleted", "sheetId", sheetId));
    }
}
