package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.entity.ActionSheet;
import com.alahlia.actionsheet.service.ActionSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Handles one-click email response buttons.
 * Recipients click a button in the email → GET request hits this endpoint
 * → response is recorded → user sees a styled confirmation page.
 */
@RestController
@RequestMapping("/api/respond")
@RequiredArgsConstructor
@Slf4j
public class ResponseController {

    private final ActionSheetService actionSheetService;

    // Secret used to generate HMAC tokens for response links
    private static final String HMAC_SECRET = "ACG-ActionSheet-2026-SecureToken";

    /**
     * Handle one-click response from email button.
     * GET because email clients follow links with GET requests.
     */
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> handleEmailResponse(
            @RequestParam("sheet") String sheetId,
            @RequestParam("email") String email,
            @RequestParam("response") String response,
            @RequestParam("token") String token) {

        log.info("📩 Email response received — Sheet: {} | Email: {} | Response: {}", sheetId, email, response);

        // Validate HMAC token
        String expectedToken = generateToken(sheetId, email, response);
        if (!expectedToken.equals(token)) {
            log.warn("❌ Invalid token for response: sheet={}, email={}", sheetId, email);
            return ResponseEntity.ok(buildResponsePage(
                    "⚠️ Invalid Link",
                    "This response link has expired or is invalid.",
                    "#e65100", false));
        }

        try {
            // Check if sheet exists and get current state
            ActionSheet sheet = actionSheetService.getActionSheetEntity(sheetId);
            if (sheet == null) {
                return ResponseEntity.ok(buildResponsePage(
                        "⚠️ Sheet Not Found",
                        "Action Sheet " + sheetId + " does not exist.",
                        "#e65100", false));
            }

            // Check if already responded
            if (sheet.getResponses() != null && sheet.getResponses().containsKey(email)) {
                String existingResponse = sheet.getResponses().get(email);
                return ResponseEntity.ok(buildResponsePage(
                        "ℹ️ Already Responded",
                        "You have already responded to this action sheet with: <strong>" +
                                existingResponse + "</strong><br><br>Your original response has been recorded.",
                        "#1976d2", true));
            }

            // Check if INFO-only recipient
            String recipientType = sheet.getRecipientTypes() != null ?
                    sheet.getRecipientTypes().get(email) : null;
            if ("INFO".equalsIgnoreCase(recipientType)) {
                return ResponseEntity.ok(buildResponsePage(
                        "ℹ️ Information Only",
                        "This action sheet was sent to you for information only. No response is required.",
                        "#1976d2", true));
            }

            // Record the response
            actionSheetService.addResponse(sheetId, email, response, email, null, 5);

            log.info("✅ Response recorded via email click — Sheet: {} | Email: {} | Response: {}",
                    sheetId, email, response);

            return ResponseEntity.ok(buildResponsePage(
                    "✅ Response Recorded",
                    "Your response <strong>\"" + response + "\"</strong> has been recorded successfully." +
                            "<br><br>Action Sheet: <code>" + sheetId + "</code>" +
                            "<br>Thank you for your prompt response.",
                    "#16a34a", true));

        } catch (Exception e) {
            log.error("Failed to process email response: {}", e.getMessage(), e);
            return ResponseEntity.ok(buildResponsePage(
                    "❌ Error",
                    "An error occurred while processing your response. Please try again or contact the administrator." +
                            "<br><br>Error: " + e.getMessage(),
                    "#dc2626", false));
        }
    }

    /**
     * Generate HMAC-SHA256 token for a response link
     */
    public static String generateToken(String sheetId, String email, String response) {
        try {
            String data = sheetId + "|" + email + "|" + response;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    /**
     * Build a response URL for embedding in emails
     */
    public static String buildResponseUrl(String baseUrl, String sheetId, String email, String response) {
        try {
            String token = generateToken(sheetId, email, response);
            return baseUrl + "/api/respond?sheet=" +
                    URLEncoder.encode(sheetId, StandardCharsets.UTF_8) +
                    "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) +
                    "&response=" + URLEncoder.encode(response, StandardCharsets.UTF_8) +
                    "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build response URL", e);
        }
    }

    /**
     * Build a styled HTML response page shown after button click
     */
    private String buildResponsePage(String title, String message, String accentColor, boolean success) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - Al-Ahlia Action Sheet</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', -apple-system, Arial, sans-serif;
                        background: linear-gradient(135deg, #f5f0ea 0%%, #e8e0d4 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .card {
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.08);
                        max-width: 480px;
                        width: 100%%;
                        overflow: hidden;
                        animation: fadeIn 0.5s ease-out;
                    }
                    @keyframes fadeIn {
                        from { opacity: 0; transform: translateY(20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    .header {
                        background: linear-gradient(135deg, #800000, #5c0000);
                        padding: 24px;
                        text-align: center;
                    }
                    .header h1 { color: white; font-size: 16px; font-weight: 600; }
                    .header p { color: rgba(255,255,255,0.7); font-size: 12px; margin-top: 4px; }
                    .body { padding: 40px 32px; text-align: center; }
                    .icon {
                        width: 72px; height: 72px;
                        border-radius: 50%%;
                        background: %s15;
                        display: flex; align-items: center; justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 36px;
                    }
                    .body h2 { color: %s; font-size: 22px; margin-bottom: 12px; }
                    .body p { color: #555; font-size: 14px; line-height: 1.6; }
                    .body code {
                        background: #f0ebe5; padding: 2px 8px;
                        border-radius: 4px; font-size: 13px;
                    }
                    .footer {
                        background: #faf8f5; padding: 16px;
                        text-align: center; border-top: 1px solid #e5e0d8;
                    }
                    .footer p { color: #999; font-size: 11px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h1>AL-AHLIA CONTRACTING GROUP</h1>
                        <p>Action Sheet System</p>
                    </div>
                    <div class="body">
                        <div class="icon">%s</div>
                        <h2>%s</h2>
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>You may close this window.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                title,
                accentColor, accentColor,
                success ? "✅" : "⚠️",
                title,
                message
        );
    }
}
