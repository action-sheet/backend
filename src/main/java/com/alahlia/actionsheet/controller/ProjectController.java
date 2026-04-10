package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.entity.Project;
import com.alahlia.actionsheet.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
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
     * Accepts paths relative to the data root, e.g. /files/ActionSheet_xyz.pdf
     * or /projects/maha/ActionSheets/ActionSheet_MAHA-260217.pdf
     */
    @GetMapping("/serve-file")
    public ResponseEntity<Resource> serveFile(@RequestParam String path) {
        // Decode and sanitize
        String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        // Prevent path traversal
        decoded = decoded.replace("..", "");

        File file = new File(dataPath, decoded);
        if (!file.exists() || !file.isFile()) {
            // Try absolute path (legacy pdfPath may be absolute)
            file = new File(decoded);
        }
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = "application/octet-stream";
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) contentType = "application/pdf";
        else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (name.endsWith(".png")) contentType = "image/png";

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
