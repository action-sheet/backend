package com.alahlia.actionsheet.migration;

import com.alahlia.actionsheet.entity.ActionSheet;
import com.alahlia.actionsheet.entity.ResponseEntry;
import com.alahlia.actionsheet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Data Migration Tool — reads legacy .dat files from the Swing app.
 *
 * The legacy classes (com.alahlia.Employee, com.alahlia.ActionSheetManager, etc.)
 * are provided as stub classes in the backend's com.alahlia package, so standard
 * ObjectInputStream can deserialize them directly.
 *
 * Data formats:
 *   employees.dat    → List<com.alahlia.Employee>
 *   actionSheets.dat → Map<String, com.alahlia.ActionSheetManager.ActionSheet>
 *   projects.dat     → Map<String, com.alahlia.ProjectManager.Project>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataMigrationTool implements CommandLineRunner {

    private final ActionSheetRepository actionSheetRepository;
    private final com.alahlia.actionsheet.repository.EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    private static final String[] DATA_PATHS = {
            "Z:\\Action Sheet System\\data",
            System.getenv("APPDATA") != null ? System.getenv("APPDATA") + "\\ActionSheetApp\\data" : null,
            "E:\\action-sheet-app\\data",
            "./data",
    };

    private final org.springframework.core.env.Environment environment;

    @Override
    public void run(String... args) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("migrate")) {
            log.info("Migration skipped. Use --spring.profiles.active=migrate to run migration.");
            return;
        }

        log.info("=".repeat(80));
        log.info("STARTING FULL DATA MIGRATION FROM LEGACY SYSTEM");
        log.info("=".repeat(80));

        File dataDir = findDataDirectory();
        if (dataDir == null) {
            log.error("DATA DIRECTORY NOT FOUND.");
            return;
        }
        log.info("Data directory: {}", dataDir.getAbsolutePath());

        try {
            migrateEmployees(dataDir);
            migrateProjects(dataDir);
            migrateActionSheets(dataDir);

            log.info("=".repeat(80));
            log.info("MIGRATION COMPLETED");
            log.info("  Employees in DB:     {}", employeeRepository.count());
            log.info("  Action Sheets in DB: {}", actionSheetRepository.count());
            log.info("=".repeat(80));
        } catch (Exception e) {
            log.error("MIGRATION FAILED", e);
        }
    }

    private File findDataDirectory() {
        for (String path : DATA_PATHS) {
            if (path == null) continue;
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) return dir;
        }
        return null;
    }

    // ===================== EMPLOYEES =====================

    @SuppressWarnings("unchecked")
    private void migrateEmployees(File dataDir) {
        File empFile = new File(dataDir, "employees.dat");
        if (!empFile.exists()) { log.warn("employees.dat not found"); return; }

        log.info("Migrating employees from {}", empFile.getAbsolutePath());
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(empFile))) {
            List<com.alahlia.Employee> legacyList = (List<com.alahlia.Employee>) ois.readObject();

            int count = 0, skipped = 0;
            for (com.alahlia.Employee le : legacyList) {
                try {
                    if (le.email == null || le.email.isBlank()) { skipped++; continue; }
                    String email = le.email.trim().toLowerCase();
                    if (employeeRepository.findById(email).isPresent()) { skipped++; continue; }

                    com.alahlia.actionsheet.entity.Employee emp = new com.alahlia.actionsheet.entity.Employee();
                    emp.setEmail(email);
                    emp.setName(le.name != null ? le.name.trim() : email);
                    emp.setDepartment(le.department != null ? le.department.trim() : "General");
                    emp.setRole(le.role != null ? le.role.trim() : "User");
                    emp.setActiveDirectory(le.activeDirectory != null ? le.activeDirectory : "");
                    emp.setAdObjectGuid(le.adObjectGuid);
                    emp.setAdDistinguishedName(le.adDistinguishedName);
                    emp.setAdSynced(le.isAdSynced);
                    emp.setGroup(le.isGroup);
                    emp.setActive(true);

                    employeeRepository.save(emp);
                    count++;
                } catch (Exception e) {
                    skipped++;
                    log.debug("  skip: {}", e.getMessage());
                }
            }
            log.info("✅ Employees: {} migrated, {} skipped (of {} total)", count, skipped, legacyList.size());
        } catch (Exception e) {
            log.error("Employee migration failed: {}", e.getMessage(), e);
        }
    }

    // ===================== PROJECTS =====================

    @SuppressWarnings("unchecked")
    private void migrateProjects(File dataDir) {
        File projFile = new File(dataDir, "projects.dat");
        if (!projFile.exists()) { log.warn("projects.dat not found"); return; }

        log.info("Migrating projects from {}", projFile.getAbsolutePath());
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(projFile))) {
            Object data = ois.readObject();
            int count = 0;

            Map<?, ?> map = null;
            if (data instanceof Map) {
                map = (Map<?, ?>) data;
            }

            if (map != null) {
                for (Object val : map.values()) {
                    if (val instanceof com.alahlia.ProjectManager.Project) {
                        com.alahlia.ProjectManager.Project lp = (com.alahlia.ProjectManager.Project) val;
                        log.info("  Project: {} (id={}, path={})", lp.name, lp.id, lp.path);

                        if (!projectRepository.findById(lp.id).isPresent()) {
                            com.alahlia.actionsheet.entity.Project proj =
                                    new com.alahlia.actionsheet.entity.Project(lp.id, lp.name);
                            proj.setPath(lp.path);
                            projectRepository.save(proj);
                        }
                        count++;
                    }
                }
            }
            log.info("✅ Projects: {} migrated to DB", count);
        } catch (Exception e) {
            log.error("Project migration failed: {}", e.getMessage());
        }
    }

    // ===================== ACTION SHEETS =====================

    @SuppressWarnings("unchecked")
    private void migrateActionSheets(File dataDir) {
        File sheetFile = new File(dataDir, "actionSheets.dat");
        if (!sheetFile.exists()) { log.warn("actionSheets.dat not found"); return; }

        log.info("Migrating action sheets from {}", sheetFile.getAbsolutePath());
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(sheetFile))) {
            // The .dat contains Map<String, ActionSheetManager.ActionSheet> directly
            Map<String, com.alahlia.ActionSheetManager.ActionSheet> legacySheets =
                    (Map<String, com.alahlia.ActionSheetManager.ActionSheet>) ois.readObject();

            log.info("Found {} action sheets in legacy data", legacySheets.size());
            int ok = 0, err = 0;

            for (Map.Entry<String, com.alahlia.ActionSheetManager.ActionSheet> entry : legacySheets.entrySet()) {
                try {
                    migrateOneSheet(entry.getValue());
                    ok++;
                    if (ok % 10 == 0) log.info("  Migrated {} sheets...", ok);
                } catch (Exception e) {
                    err++;
                    log.error("  Failed {}: {}", entry.getKey(), e.getMessage());
                }
            }
            log.info("✅ Action Sheets: {} migrated, {} errors", ok, err);
        } catch (Exception e) {
            log.error("Action sheet migration failed: {}", e.getMessage(), e);
        }
    }

    private void migrateOneSheet(com.alahlia.ActionSheetManager.ActionSheet ls) {
        if (actionSheetRepository.findById(ls.id).isPresent()) return;

        ActionSheet s = new ActionSheet();
        s.setId(ls.id);
        s.setTitle(ls.title != null ? ls.title : "Untitled");
        s.setStatus(ls.status != null ? ls.status : "DRAFT");
        s.setPdfPath(ls.pdfPath);
        s.setProjectId(ls.projectId);
        s.setCreatedDate(toLocal(ls.createdDate));
        s.setDueDate(toLocal(ls.dueDate));
        s.setWorkflowState(toWfState(ls.workflowState));
        s.setDeleted(ls.isDeleted);
        s.setDeletedAt(toLocal(ls.deletedAt));
        s.setDeletedBy(ls.deletedBy);
        s.setLastModified(ls.lastModified);
        s.setHasConflict(ls.hasConflict);
        s.setConflictSeverity(ls.conflictSeverity);
        s.setOverriddenBy(ls.overriddenBy);
        s.setOverrideNote(ls.overrideNote);

        s.setAssignedTo(safe(ls.assignedTo));
        s.setResponses(safe(ls.responses));
        s.setOthersEmails(safe(ls.othersEmails));
        s.setRecipientTypes(safe(ls.recipientTypes));
        s.setUserStatuses(safe(ls.userStatuses));
        s.setFormData(safeObj(ls.formData));

        s = actionSheetRepository.save(s);

        // Response history
        if (ls.responseHistory != null) {
            for (Map.Entry<String, List<com.alahlia.ActionSheetManager.ResponseEntry>> entry : ls.responseHistory.entrySet()) {
                for (com.alahlia.ActionSheetManager.ResponseEntry lr : entry.getValue()) {
                    ResponseEntry re = new ResponseEntry();
                    re.setActionSheet(s);
                    re.setEmail(entry.getKey());
                    re.setResponse(lr.response);
                    re.setTimestamp(toLocal(lr.timestamp));
                    re.setOverwritten(lr.isOverwritten);
                    re.setRawContent(lr.rawContent);
                    re.setSenderUserId(lr.senderUserId);
                    re.setSenderRole(lr.senderRole);
                    re.setSenderHierarchyLevel(lr.senderHierarchyLevel);
                    s.getResponseHistory().add(re);
                }
            }
        }

        // Conflict events
        if (ls.conflictLog != null) {
            for (com.alahlia.ConflictEvent lce : ls.conflictLog) {
                com.alahlia.actionsheet.entity.ConflictEvent ce = new com.alahlia.actionsheet.entity.ConflictEvent();
                ce.setActionSheet(s);
                ce.setTimestamp(toLocal(lce.detectedAt));
                ce.setSeverity(lce.severity);
                ce.setDescription(lce.resolutionMethod);
                ce.setResolvedBy(lce.resolvedBy);
                s.getConflictLog().add(ce);
            }
        }

        // Conflict threads
        if (ls.conflictThreads != null) {
            for (com.alahlia.ConflictThread lct : ls.conflictThreads) {
                if (lct.messages != null) {
                    for (com.alahlia.ConflictThread.ConflictMessage lm : lct.messages) {
                        com.alahlia.actionsheet.entity.ConflictThread ct = new com.alahlia.actionsheet.entity.ConflictThread();
                        ct.setActionSheet(s);
                        ct.setTimestamp(toLocal(lm.timestamp));
                        ct.setAuthor(lm.senderUserId);
                        ct.setMessage(lm.text);
                        s.getConflictThreads().add(ct);
                    }
                }
            }
        }

        actionSheetRepository.save(s);
    }

    // ===================== UTILITIES =====================

    private LocalDateTime toLocal(Date d) {
        return d != null ? d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
    }

    private ActionSheet.WorkflowState toWfState(Object ws) {
        if (ws == null) return ActionSheet.WorkflowState.DRAFT;
        try { return ActionSheet.WorkflowState.valueOf(ws.toString()); }
        catch (Exception e) { return ActionSheet.WorkflowState.DRAFT; }
    }

    private Map<String, String> safe(Map<String, String> m) { return m != null ? new HashMap<>(m) : new HashMap<>(); }
    private Map<String, Object> safeObj(Map<String, Object> m) { return m != null ? new HashMap<>(m) : new HashMap<>(); }
}
