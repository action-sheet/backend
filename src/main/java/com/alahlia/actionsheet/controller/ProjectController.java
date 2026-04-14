package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.entity.Project;
import com.alahlia.actionsheet.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @Value("${app.data-path:./data}")
    private String dataPath;

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable String id) {
        Project project = projectService.getProject(id);
        return project != null ? ResponseEntity.ok(project) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return projectService.createProject(project);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable String id, @RequestBody Project project) {
        Project updated = projectService.updateProject(id, project);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        boolean deleted = projectService.deleteProject(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * List files in a project's ActionSheets folder.
     */
    @GetMapping("/{id}/files")
    public ResponseEntity<List<String>> getProjectFiles(@PathVariable String id) {
        return ResponseEntity.ok(projectService.getProjectFiles(id));
    }

    /**
     * Serve a file from the data directory (PDFs, attachments, etc.)
     *
     * OPTIMIZED for speed:
     * - HTTP Range requests (206 Partial Content) for streaming/progressive rendering
     * - ETag + aggressive caching (7 days) so repeat views are instant
     * - Streaming output with 64KB buffer (no full file buffering in memory)
     * - Proper Content-Length for browser progress bars
     */
    @GetMapping("/serve-file")
    public void serveFile(@RequestParam String path,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        // Decode and sanitize
        String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        decoded = decoded.replace("..", "");

        // Clean up the path
        String cleanPath = decoded;

        // Remove network path prefix like \\actionsheet\E\ or \\actionsheet\Z\
        if (cleanPath.startsWith("\\\\actionsheet\\")) {
            int driveIndex = cleanPath.indexOf("\\", 2);
            if (driveIndex > 0 && driveIndex + 2 < cleanPath.length()) {
                cleanPath = cleanPath.substring(driveIndex + 2);
            }
        }

        // Normalize slashes
        cleanPath = cleanPath.replace("\\", "/");

        // Resolve the file — try absolute, then relative to dataPath
        File file = null;
        if (cleanPath.matches("^[A-Za-z]:/.*")) {
            file = new File(cleanPath);
        }
        if (file == null || !file.exists() || !file.isFile()) {
            file = new File(dataPath, cleanPath);
        }
        if (!file.exists() || !file.isFile()) {
            file = new File(decoded);
        }
        if (!file.exists() || !file.isFile()) {
            file = new File(cleanPath);
        }

        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        long fileSize = file.length();
        String fileName = file.getName();
        String contentType = "application/octet-stream";
        String nameLower = fileName.toLowerCase();
        if (nameLower.endsWith(".pdf")) contentType = "application/pdf";
        else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (nameLower.endsWith(".png")) contentType = "image/png";

        // ETag for caching — repeat views are instant (304 Not Modified)
        String etag = "\"" + fileSize + "-" + file.lastModified() + "\"";
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // === Handle Range requests for streaming/progressive PDF rendering ===
        String rangeHeader = request.getHeader("Range");
        long start = 0;
        long end = fileSize - 1;
        boolean isRangeRequest = false;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            isRangeRequest = true;
            String rangeValue = rangeHeader.substring(6).trim();
            String[] parts = rangeValue.split("-");
            try {
                if (!parts[0].isEmpty()) {
                    start = Long.parseLong(parts[0]);
                }
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    end = Long.parseLong(parts[1]);
                }
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            if (start > end || start >= fileSize) {
                response.setHeader("Content-Range", "bytes */" + fileSize);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            end = Math.min(end, fileSize - 1);
        }

        long contentLength = end - start + 1;

        // Set response headers
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", etag);
        response.setHeader("Cache-Control", "public, max-age=604800, immutable"); // 7 days
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("ngrok-skip-browser-warning", "true");

        if (isRangeRequest) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        // Stream the file with 64KB buffer — no full file buffering in memory
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {

            raf.seek(start);
            byte[] buffer = new byte[65536]; // 64KB chunks
            long remaining = contentLength;

            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int bytesRead = raf.read(buffer, 0, toRead);
                if (bytesRead <= 0) break;
                out.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            out.flush();
        }
    }
}
