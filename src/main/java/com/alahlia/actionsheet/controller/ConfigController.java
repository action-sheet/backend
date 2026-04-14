package com.alahlia.actionsheet.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only controller for reading/updating application configuration.
 * Provides endpoints for Email (SMTP/IMAP), AD, and Notification settings.
 * Note: Changes here update the running instance only.
 * For persistent changes, the application.yml must be modified and the server restarted.
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ConfigController {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${app.email.from:}")
    private String emailFrom;

    @Value("${app.email.receiver.enabled:false}")
    private boolean imapEnabled;

    @Value("${app.email.imap.host:imap.gmail.com}")
    private String imapHost;

    @Value("${app.email.imap.port:993}")
    private String imapPort;

    @Value("${app.ad.enabled:false}")
    private boolean adEnabled;

    @Value("${app.ad.domain:}")
    private String adDomain;

    @Value("${app.ad.server:}")
    private String adServer;

    @Value("${app.email.tracking-enabled:false}")
    private boolean trackingEnabled;

    @Value("${app.auto-refresh-interval:30000}")
    private int autoRefreshInterval;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.data-path:E:/Action Sheet System/data}")
    private String dataPath;

    // ── Email Config ──

    @GetMapping("/email")
    public ResponseEntity<Map<String, Object>> getEmailConfig() {
        return ResponseEntity.ok(Map.of(
                "smtpHost", smtpHost,
                "smtpPort", smtpPort,
                "smtpUsername", smtpUsername,
                "emailFrom", emailFrom,
                "imapEnabled", imapEnabled,
                "imapHost", imapHost,
                "imapPort", imapPort,
                "trackingEnabled", trackingEnabled
        ));
    }

    // ── AD Config ──

    @GetMapping("/ad")
    public ResponseEntity<Map<String, Object>> getAdConfig() {
        return ResponseEntity.ok(Map.of(
                "adEnabled", adEnabled,
                "adDomain", adDomain != null ? adDomain : "",
                "adServer", adServer != null ? adServer : ""
        ));
    }

    // ── Notifications Config ──

    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> getNotificationsConfig() {
        return ResponseEntity.ok(Map.of(
                "autoRefreshInterval", autoRefreshInterval,
                "baseUrl", baseUrl,
                "dataPath", dataPath
        ));
    }

    // ── System Info ──

    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Runtime rt = Runtime.getRuntime();
        return ResponseEntity.ok(Map.of(
                "javaVersion", System.getProperty("java.version"),
                "osName", System.getProperty("os.name"),
                "totalMemoryMB", rt.totalMemory() / (1024 * 1024),
                "freeMemoryMB", rt.freeMemory() / (1024 * 1024),
                "usedMemoryMB", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024),
                "maxMemoryMB", rt.maxMemory() / (1024 * 1024),
                "availableProcessors", rt.availableProcessors(),
                "dataPath", dataPath,
                "baseUrl", baseUrl
        ));
    }
}
