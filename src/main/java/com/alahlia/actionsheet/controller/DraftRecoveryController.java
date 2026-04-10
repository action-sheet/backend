package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.dto.DtoMapper;
import com.alahlia.actionsheet.entity.ActionSheet;
import com.alahlia.actionsheet.service.DraftRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints for draft recovery.
 * Lists all backup snapshots and allows restoring lost drafts.
 */
@RestController
@RequestMapping("/api/admin/draft-recovery")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class DraftRecoveryController {

    private final DraftRecoveryService draftRecoveryService;

    @GetMapping
    public ResponseEntity<List<DraftRecoveryService.DraftSnapshot>> listSnapshots() {
        return ResponseEntity.ok(draftRecoveryService.listSnapshots());
    }

    @PostMapping("/restore/{fileName}")
    public ResponseEntity<?> restoreSnapshot(@PathVariable String fileName) {
        try {
            ActionSheet restored = draftRecoveryService.restoreFromSnapshot(fileName);
            log.info("Admin restored draft from snapshot: {} -> sheet {}", fileName, restored.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sheetId", restored.getId(),
                    "title", restored.getTitle(),
                    "message", "Draft restored successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to restore snapshot {}: {}", fileName, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<?> deleteSnapshot(@PathVariable String fileName) {
        boolean deleted = draftRecoveryService.deleteSnapshot(fileName);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.notFound().build();
    }
}
