package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.service.DocumentRepositoryService;
import com.alahlia.actionsheet.service.DocumentRepositoryService.RepoDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/repository")
@RequiredArgsConstructor
@Slf4j
public class DocumentRepositoryController {

    private final DocumentRepositoryService repoService;

    @GetMapping("/documents/{dateKey}")
    public ResponseEntity<List<RepoDocument>> getDocuments(
            @PathVariable String dateKey,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        List<RepoDocument> docs = new ArrayList<>(repoService.getDocumentsForDate(dateKey));
        if (includeDeleted) {
            docs.addAll(repoService.getDeletedDocumentsForDate(dateKey));
        }
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/dates/{year}/{month}")
    public ResponseEntity<Set<Integer>> getDatesWithDocuments(
            @PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(repoService.getDatesWithDocuments(year, month));
    }

    @PostMapping("/upload/{dateKey}")
    public ResponseEntity<List<RepoDocument>> uploadDocuments(
            @PathVariable String dateKey,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(defaultValue = "Current User") String uploaderName) {
        List<RepoDocument> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                RepoDocument doc = repoService.uploadDocument(
                        dateKey, file.getOriginalFilename(), file.getBytes(), uploaderName);
                uploaded.add(doc);
            } catch (Exception e) {
                log.error("Failed to upload {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }
        return ResponseEntity.ok(uploaded);
    }

    @GetMapping("/download/{dateKey}/{fileName}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable String dateKey, @PathVariable String fileName) {
        File file = repoService.getDocumentFile(dateKey, fileName);
        if (!file.exists()) return ResponseEntity.notFound().build();
        Resource resource = new FileSystemResource(file);
        String contentType = fileName.toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/octet-stream";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/documents/{dateKey}/{docId}")
    public ResponseEntity<Map<String, Boolean>> deleteDocument(
            @PathVariable String dateKey, @PathVariable String docId,
            @RequestParam(defaultValue = "Unknown") String deletedBy) {
        return ResponseEntity.ok(Map.of("success", repoService.deleteDocument(dateKey, docId, deletedBy)));
    }

    @PostMapping("/restore/{dateKey}/{docId}")
    public ResponseEntity<Map<String, Boolean>> restoreDocument(
            @PathVariable String dateKey, @PathVariable String docId) {
        return ResponseEntity.ok(Map.of("success", repoService.restoreDocument(dateKey, docId)));
    }
}
