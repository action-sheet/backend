package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.entity.Project;
import com.alahlia.actionsheet.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Value("${app.data-path:./data}")
    private String dataPath;

    public List<Project> getAllProjects() {
        return projectRepository.findByActiveTrueOrderByNameAsc();
    }

    public Project getProject(String id) {
        return projectRepository.findById(id).orElse(null);
    }

    public Project createProject(Project project) {
        // Generate ID if not set
        if (project.getId() == null || project.getId().isBlank()) {
            project.setId("PROJ_" + System.currentTimeMillis());
        }
        if (project.getCreatedDate() == null) {
            project.setCreatedDate(LocalDateTime.now());
        }
        project.setActive(true);

        // Create physical folder at data/projects/<name>
        String safeName = project.getName().replaceAll("[^a-zA-Z0-9._\\-() ]", "_");
        String projectPath = dataPath + File.separator + "projects" + File.separator + safeName;
        File projectDir = new File(projectPath);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
            log.info("Created project folder: {}", projectPath);
        }
        // Create ActionSheets subfolder
        File actionSheetsDir = new File(projectDir, "ActionSheets");
        if (!actionSheetsDir.exists()) {
            actionSheetsDir.mkdirs();
        }
        project.setPath(projectPath);

        log.info("Creating project: {} - {} at {}", project.getId(), project.getName(), projectPath);
        return projectRepository.save(project);
    }

    public Project updateProject(String id, Project updatedProject) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));
        
        existing.setName(updatedProject.getName());
        existing.setDescription(updatedProject.getDescription());
        
        log.info("Updated project: {}", id);
        return projectRepository.save(existing);
    }

    public boolean deleteProject(String id) {
        Project project = projectRepository.findById(id).orElse(null);
        if (project != null) {
            project.setActive(false);
            projectRepository.save(project);
            log.info("Deactivated project: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Get list of files in a project folder.
     */
    public List<String> getProjectFiles(String projectId) {
        Project p = getProject(projectId);
        if (p == null || p.getPath() == null) return List.of();
        File actionSheetsDir = new File(p.getPath(), "ActionSheets");
        if (!actionSheetsDir.exists()) return List.of();
        String[] files = actionSheetsDir.list();
        return files != null ? List.of(files) : List.of();
    }
}
