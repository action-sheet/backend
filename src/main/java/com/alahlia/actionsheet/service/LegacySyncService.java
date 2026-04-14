package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.entity.ActionSheet;
import com.alahlia.actionsheet.entity.ResponseEntry;
import com.alahlia.actionsheet.repository.ActionSheetRepository;
import com.alahlia.actionsheet.repository.EmployeeRepository;
import com.alahlia.actionsheet.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.io.*;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronises the web-app database BACK to the legacy .dat files at
 * Z:\Action Sheet System\data\ so that the Java Swing clients can
 * continue to read the same data.
 *
 * Serialization format matches exactly:
 *   employees.dat    → List<com.alahlia.Employee>
 *   actionSheets.dat → Map<String, com.alahlia.ActionSheetManager.ActionSheet>
 *   projects.dat     → Map<String, com.alahlia.ProjectManager.Project>
 */
// @Service
@RequiredArgsConstructor
@Slf4j
public class LegacySyncService {

    private final ActionSheetRepository actionSheetRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    @Value("${app.data-path:E:/Action Sheet System/data}")
    private String dataPath;

    @PostConstruct
    public void init() {
        File dir = new File(dataPath);
        if (!dir.exists()) {
            dir.mkdirs();
            log.warn("Created missing data directory: {}", dataPath);
        }
    }

    /**
     * Full sync every 30 seconds — keeps legacy .dat files up to date.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    public void scheduledSync() {
        if (employeeRepository.count() == 0 && actionSheetRepository.count() == 0) {
            log.warn("DB is empty. Skipping sync to legacy to prevent data wipe.");
            return;
        }
        syncAll();
    }

    public void syncAll() {
        try {
            syncEmployees();
            syncActionSheets();
            syncProjects();
        } catch (Exception e) {
            log.error("Legacy sync failed: {}", e.getMessage());
        }
    }

    // ======================= EMPLOYEES =======================

    public void syncEmployees() {
        try {
            List<com.alahlia.actionsheet.entity.Employee> dbEmployees = employeeRepository.findAll();
            List<com.alahlia.Employee> legacyList = new ArrayList<>();

            for (com.alahlia.actionsheet.entity.Employee dbEmp : dbEmployees) {
                com.alahlia.Employee legacy = new com.alahlia.Employee();
                legacy.name = dbEmp.getName();
                legacy.email = dbEmp.getEmail();
                legacy.department = dbEmp.getDepartment();
                legacy.activeDirectory = dbEmp.getActiveDirectory();
                legacy.role = dbEmp.getRole();
                legacy.adObjectGuid = dbEmp.getAdObjectGuid();
                legacy.adDistinguishedName = dbEmp.getAdDistinguishedName();
                legacy.lastAdSyncTime = dbEmp.getLastAdSyncTime() != null
                        ? Date.from(dbEmp.getLastAdSyncTime().atZone(ZoneId.systemDefault()).toInstant()) : null;
                legacy.isAdSynced = dbEmp.isAdSynced();
                legacy.isGroup = dbEmp.isGroup();
                legacyList.add(legacy);
            }

            writeObject(new File(dataPath, "employees.dat"), legacyList);
            log.debug("Synced {} employees to legacy", legacyList.size());
        } catch (Exception e) {
            log.error("Failed to sync employees to legacy: {}", e.getMessage());
        }
    }

    // ======================= ACTION SHEETS =======================

    public void syncActionSheets() {
        try {
            List<ActionSheet> dbSheets = actionSheetRepository.findAll();
            Map<String, com.alahlia.ActionSheetManager.ActionSheet> legacyMap = new ConcurrentHashMap<>();

            for (ActionSheet dbSheet : dbSheets) {
                com.alahlia.ActionSheetManager.ActionSheet legacy = new com.alahlia.ActionSheetManager.ActionSheet();
                legacy.id = dbSheet.getId();
                legacy.title = dbSheet.getTitle();
                legacy.createdDate = dbSheet.getCreatedDate() != null
                        ? Date.from(dbSheet.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant()) : new Date();
                legacy.dueDate = dbSheet.getDueDate() != null
                        ? Date.from(dbSheet.getDueDate().atZone(ZoneId.systemDefault()).toInstant()) : null;
                legacy.status = dbSheet.getStatus();
                legacy.assignedTo = dbSheet.getAssignedTo() != null ? new ConcurrentHashMap<>(dbSheet.getAssignedTo()) : new ConcurrentHashMap<>();
                legacy.responses = dbSheet.getResponses() != null ? new ConcurrentHashMap<>(dbSheet.getResponses()) : new ConcurrentHashMap<>();
                legacy.pdfPath = dbSheet.getPdfPath();
                legacy.formData = dbSheet.getFormData() != null ? new ConcurrentHashMap<>(dbSheet.getFormData()) : new ConcurrentHashMap<>();
                legacy.projectId = dbSheet.getProjectId();
                legacy.recipientTypes = dbSheet.getRecipientTypes() != null ? new ConcurrentHashMap<>(dbSheet.getRecipientTypes()) : new ConcurrentHashMap<>();
                legacy.isDeleted = dbSheet.isDeleted();
                legacy.deletedAt = dbSheet.getDeletedAt() != null
                        ? Date.from(dbSheet.getDeletedAt().atZone(ZoneId.systemDefault()).toInstant()) : null;
                legacy.deletedBy = dbSheet.getDeletedBy();
                legacy.lastModified = dbSheet.getLastModified() != null
                        ? dbSheet.getLastModified() : System.currentTimeMillis();

                // Workflow state
                if (dbSheet.getWorkflowState() != null) {
                    legacy.workflowState = com.alahlia.ActionSheetManager.ActionSheet.WorkflowState.valueOf(dbSheet.getWorkflowState().name());
                }

                // Conflict fields
                legacy.userStatuses = dbSheet.getUserStatuses() != null ? new ConcurrentHashMap<>(dbSheet.getUserStatuses()) : new ConcurrentHashMap<>();
                legacy.hasConflict = dbSheet.isHasConflict();
                legacy.conflictSeverity = dbSheet.getConflictSeverity();
                legacy.overriddenBy = dbSheet.getOverriddenBy();
                legacy.overrideNote = dbSheet.getOverrideNote();

                // Response history
                if (dbSheet.getResponseHistory() != null && !dbSheet.getResponseHistory().isEmpty()) {
                    Map<String, List<com.alahlia.ActionSheetManager.ResponseEntry>> historyMap = new ConcurrentHashMap<>();
                    for (ResponseEntry re : dbSheet.getResponseHistory()) {
                        com.alahlia.ActionSheetManager.ResponseEntry legacyRe = new com.alahlia.ActionSheetManager.ResponseEntry();
                        legacyRe.email = re.getEmail();
                        legacyRe.response = re.getResponse();
                        legacyRe.timestamp = re.getTimestamp() != null
                                ? Date.from(re.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()) : null;
                        legacyRe.rawContent = re.getRawContent();
                        legacyRe.senderUserId = re.getSenderUserId();
                        legacyRe.senderRole = re.getSenderRole();
                        historyMap.computeIfAbsent(re.getEmail() != null ? re.getEmail() : "unknown", k -> new ArrayList<>()).add(legacyRe);
                    }
                    legacy.responseHistory = historyMap;
                } else {
                    legacy.responseHistory = new ConcurrentHashMap<>();
                }

                legacy.conflictLog = new ArrayList<>();
                legacy.conflictThreads = new ArrayList<>();

                legacyMap.put(legacy.id, legacy);
            }

            writeObject(new File(dataPath, "actionSheets.dat"), legacyMap);
            log.debug("Synced {} action sheets to legacy", legacyMap.size());
        } catch (Exception e) {
            log.error("Failed to sync action sheets to legacy: {}", e.getMessage());
        }
    }

    // ======================= PROJECTS =======================

    public void syncProjects() {
        try {
            List<com.alahlia.actionsheet.entity.Project> dbProjects = projectRepository.findAll();
            Map<String, com.alahlia.ProjectManager.Project> legacyMap = new ConcurrentHashMap<>();

            for (com.alahlia.actionsheet.entity.Project dbProj : dbProjects) {
                com.alahlia.ProjectManager.Project legacy = new com.alahlia.ProjectManager.Project();
                legacy.id = dbProj.getId();
                legacy.name = dbProj.getName();
                legacy.path = dbProj.getPath();
                legacy.color = new Color(0, 122, 255); // Default blue
                legacyMap.put(legacy.id, legacy);
            }

            writeObject(new File(dataPath, "projects.dat"), legacyMap);
            log.debug("Synced {} projects to legacy", legacyMap.size());
        } catch (Exception e) {
            log.error("Failed to sync projects to legacy: {}", e.getMessage());
        }
    }

    // ======================= I/O =======================

    private void writeObject(File file, Object obj) throws IOException {
        // Write to .tmp first, then rename (atomic)
        File tmpFile = new File(file.getPath() + ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmpFile))) {
            oos.writeObject(obj);
        }
        // Replace original
        if (file.exists()) {
            file.delete();
        }
        tmpFile.renameTo(file);
    }
}
