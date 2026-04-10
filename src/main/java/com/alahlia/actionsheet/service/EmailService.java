package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.controller.ResponseController;
import com.alahlia.actionsheet.entity.ActionSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Email service using Gmail SMTP.
 * Sends professional action sheet notifications with embedded logo,
 * recipient listing, and one-click response buttons.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:gemis6292@gmail.com}")
    private String fromEmail;

    @Value("${app.email.tracking-enabled:false}")
    private boolean trackingEnabled;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Send action sheet email to all recipients with PDF attachment
     */
    public void sendActionSheetEmail(ActionSheet sheet, List<String> recipients,
                                     Map<String, String> recipientNames,
                                     List<File> attachments) {
        int successCount = 0;
        int failCount = 0;

        for (String email : recipients) {
            try {
                sendSingleEmail(sheet, email, recipientNames.getOrDefault(email, email), attachments);
                successCount++;
                log.info("Email sent to: {}", email);
            } catch (Exception e) {
                failCount++;
                log.error("Failed to send email to {}: {}", email, e.getMessage());
            }
        }

        log.info("Email batch complete for sheet {}: {} sent, {} failed",
                sheet.getId(), successCount, failCount);
    }

    private void sendSingleEmail(ActionSheet sheet, String toEmail, String recipientName,
                                 List<File> attachments) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, "Al-Ahlia Contracting Group");
        helper.setTo(toEmail);
        helper.setSubject(sheet.getTitle() + " [Ref:" + sheet.getId() + "]");

        String htmlBody = buildEmailBody(sheet, recipientName, toEmail);
        helper.setText(buildPlainTextBody(sheet, toEmail), htmlBody);

        // Embed logo as inline CID attachment
        try {
            ClassPathResource logo = new ClassPathResource("static/acg_logo.jpg");
            if (logo.exists()) {
                helper.addInline("acglogo", logo, "image/jpeg");
            }
        } catch (Exception e) {
            log.debug("Could not embed logo: {}", e.getMessage());
        }

        if (attachments != null) {
            for (File file : attachments) {
                if (file != null && file.exists()) {
                    helper.addAttachment(file.getName(), file);
                }
            }
        }

        mailSender.send(message);
    }

    // ═══════════════════════════════════════════════════════════════
    // EMAIL TEMPLATE — Clean, professional, no emojis
    // ═══════════════════════════════════════════════════════════════

    private String buildEmailBody(ActionSheet sheet, String recipientName, String email) {
        StringBuilder h = new StringBuilder();

        boolean isInfoOnly = "INFO".equalsIgnoreCase(
                sheet.getRecipientTypes() != null ? sheet.getRecipientTypes().get(email) : null);

        h.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        h.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        h.append("</head>");
        h.append("<body style=\"margin:0; padding:0; background:#f4f1ec; font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;\">");

        // Container
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f1ec;\">");
        h.append("<tr><td align=\"center\" style=\"padding:24px 16px;\">");
        h.append("<table role=\"presentation\" width=\"620\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff; border-radius:4px; overflow:hidden; box-shadow:0 1px 4px rgba(0,0,0,0.08);\">");

        // ── HEADER ─────────────────────────────────────
        h.append("<tr><td style=\"background:#800000; padding:28px 40px; text-align:center;\">");
        h.append("<img src=\"cid:acglogo\" alt=\"ACG\" width=\"80\" style=\"display:block; margin:0 auto 12px; border-radius:4px;\" />");
        h.append("<div style=\"color:#ffffff; font-size:18px; font-weight:700; letter-spacing:1.5px;\">AL-AHLIA CONTRACTING GROUP</div>");
        h.append("<div style=\"color:rgba(255,255,255,0.6); font-size:11px; letter-spacing:1px; margin-top:4px;\">ACTION SHEET NOTIFICATION</div>");
        h.append("</td></tr>");

        // ── BODY ───────────────────────────────────────
        h.append("<tr><td style=\"padding:32px 40px;\">");

        // Greeting
        h.append("<p style=\"color:#222; font-size:15px; margin:0 0 6px;\">Dear <strong>")
                .append(recipientName != null ? recipientName : "Sir/Madam")
                .append("</strong>,</p>");
        h.append("<p style=\"color:#555; font-size:13px; margin:0 0 24px; line-height:1.6;\">")
                .append("An action sheet has been sent for your attention. Please find the details below:")
                .append("</p>");

        // ── Details Table ──
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#faf9f7; border:1px solid #e8e3dc; border-radius:4px; margin-bottom:24px;\">");
        h.append("<tr><td style=\"padding:16px 20px;\">");
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:13px;\">");

        addRow(h, "Subject", sheet.getTitle(), true);
        addRow(h, "Reference", sheet.getId(), false);

        if (sheet.getFormData() != null) {
            Object from = sheet.getFormData().get("from");
            Object refNo = sheet.getFormData().get("refNo");
            Object resp = sheet.getFormData().get("response");
            if (from != null && !from.toString().isBlank()) addRow(h, "From", from.toString(), false);
            if (refNo != null && !refNo.toString().isBlank()) addRow(h, "Ref. No.", refNo.toString(), false);
            if (resp != null && !resp.toString().isBlank()) addRow(h, "Response Required", resp.toString(), false);
        }

        h.append("</table></td></tr></table>");

        // ── Recipients List ──
        if (sheet.getAssignedTo() != null && !sheet.getAssignedTo().isEmpty()) {
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:24px;\">");
            h.append("<tr><td style=\"padding-bottom:10px; color:#800000; font-size:12px; font-weight:700; letter-spacing:0.5px; text-transform:uppercase; border-bottom:2px solid #800000;\">Distribution List</td></tr>");

            for (Map.Entry<String, String> entry : sheet.getAssignedTo().entrySet()) {
                String recipEmail = entry.getKey();
                String recipName = entry.getValue();
                String type = sheet.getRecipientTypes() != null ?
                        sheet.getRecipientTypes().getOrDefault(recipEmail, "ACTION") : "ACTION";
                boolean isAction = "ACTION".equalsIgnoreCase(type);
                boolean isYou = recipEmail.equalsIgnoreCase(email);

                h.append("<tr><td style=\"padding:8px 0; border-bottom:1px solid #f0ece6;\">");
                h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>");

                // Name
                h.append("<td style=\"color:#333; font-size:13px;\">");
                h.append("<strong>").append(recipName).append("</strong>");
                if (isYou) {
                    h.append(" <span style=\"color:#800000; font-size:11px; font-weight:600;\">(You)</span>");
                }
                h.append("</td>");

                // Badge
                h.append("<td align=\"right\">");
                h.append("<span style=\"display:inline-block; padding:3px 12px; border-radius:3px; font-size:10px; font-weight:700; letter-spacing:0.3px; ");
                if (isAction) {
                    h.append("background:#800000; color:#ffffff;\">");
                    h.append("FOR ACTION");
                } else {
                    h.append("background:#e8e3dc; color:#666;\">");
                    h.append("FOR INFORMATION");
                }
                h.append("</span></td>");

                h.append("</tr></table></td></tr>");
            }
            h.append("</table>");
        }

        // ── Response Section ──
        if (!isInfoOnly) {
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f5f0e8; border:1px solid #e0d8c8; border-radius:4px; margin-bottom:24px;\">");
            h.append("<tr><td style=\"padding:20px 24px;\">");

            h.append("<div style=\"color:#333; font-size:14px; font-weight:700; margin-bottom:4px;\">ACTION REQUIRED</div>");
            h.append("<div style=\"color:#777; font-size:12px; margin-bottom:18px;\">Click a button below to submit your response:</div>");

            // Buttons grid — 2 columns
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");

            // Row 1
            h.append("<tr>");
            h.append(btn(sheet.getId(), email, "ACTION TAKEN"));
            h.append("<td width=\"8\"></td>");
            h.append(btn(sheet.getId(), email, "COMPLETED"));
            h.append("</tr>");
            h.append("<tr><td colspan=\"3\" height=\"8\"></td></tr>");

            // Row 2
            h.append("<tr>");
            h.append(btn(sheet.getId(), email, "IN PROGRESS"));
            h.append("<td width=\"8\"></td>");
            h.append(btn(sheet.getId(), email, "APPROVED"));
            h.append("</tr>");
            h.append("<tr><td colspan=\"3\" height=\"8\"></td></tr>");

            // Row 3
            h.append("<tr>");
            h.append(btn(sheet.getId(), email, "NOTED"));
            h.append("<td width=\"8\"></td>");
            h.append(btn(sheet.getId(), email, "NEEDS REVIEW"));
            h.append("</tr>");
            h.append("<tr><td colspan=\"3\" height=\"8\"></td></tr>");

            // Row 4
            h.append("<tr>");
            h.append(btn(sheet.getId(), email, "REJECTED"));
            h.append("<td width=\"8\"></td>");
            h.append("<td></td>");
            h.append("</tr>");

            h.append("</table>");
            h.append("</td></tr></table>");
        } else {
            // Info-only notice
            h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-left:3px solid #800000; margin-bottom:24px;\">");
            h.append("<tr><td style=\"padding:14px 20px; background:#faf9f7;\">");
            h.append("<div style=\"color:#800000; font-size:13px; font-weight:700; margin-bottom:4px;\">FOR YOUR INFORMATION</div>");
            h.append("<div style=\"color:#666; font-size:12px;\">This action sheet is for your information only. No response is required.</div>");
            h.append("</td></tr></table>");
        }

        // Attachment note
        h.append("<p style=\"color:#999; font-size:11px; margin:0; text-align:center;\">The Action Sheet PDF is attached to this email.</p>");

        h.append("</td></tr>");

        // ── FOOTER ─────────────────────────────────────
        h.append("<tr><td style=\"background:#f4f1ec; padding:16px 40px; border-top:1px solid #e8e3dc;\">");
        h.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
        h.append("<td style=\"color:#999; font-size:10px;\">Al-Ahlia Contracting Group</td>");
        h.append("<td align=\"right\" style=\"color:#999; font-size:10px; font-family:monospace;\">").append(sheet.getId()).append("</td>");
        h.append("</tr></table>");
        if (!isInfoOnly) {
            h.append("<p style=\"color:#bbb; font-size:10px; margin:8px 0 0; text-align:center;\">Click the response buttons above to respond directly. No reply email needed.</p>");
        }
        h.append("</td></tr>");

        h.append("</table>"); // end card
        h.append("</td></tr></table>"); // end outer
        h.append("</body></html>");

        return h.toString();
    }

    private void addRow(StringBuilder h, String label, String value, boolean bold) {
        h.append("<tr>");
        h.append("<td style=\"padding:5px 0; color:#888; width:100px; vertical-align:top;\">").append(label).append("</td>");
        h.append("<td style=\"padding:5px 0; color:#222;");
        if (bold) h.append(" font-weight:700;");
        h.append("\">").append(value != null ? value : "—").append("</td>");
        h.append("</tr>");
    }

    private String btn(String sheetId, String email, String label) {
        String url = ResponseController.buildResponseUrl(baseUrl, sheetId, email, label);
        return "<td width=\"50%\">" +
                "<a href=\"" + url + "\" style=\"display:block; background:#ffffff; " +
                "border:1px solid #c0b8a8; color:#333; text-decoration:none; padding:12px 8px; " +
                "border-radius:4px; text-align:center; font-size:12px; font-weight:600; " +
                "letter-spacing:0.3px;\">" +
                label + "</a></td>";
    }

    private String buildPlainTextBody(ActionSheet sheet, String toEmail) {
        StringBuilder t = new StringBuilder();
        t.append("AL-AHLIA CONTRACTING GROUP\n");
        t.append("Action Sheet Notification\n");
        t.append("------------------------------------------\n\n");
        t.append("Subject: ").append(sheet.getTitle()).append("\n");
        t.append("Reference: ").append(sheet.getId()).append("\n");

        if (sheet.getFormData() != null) {
            Object from = sheet.getFormData().get("from");
            Object refNo = sheet.getFormData().get("refNo");
            if (from != null) t.append("From: ").append(from).append("\n");
            if (refNo != null) t.append("Ref. No.: ").append(refNo).append("\n");
        }

        // Recipients
        t.append("\nDistribution List:\n");
        if (sheet.getAssignedTo() != null && sheet.getRecipientTypes() != null) {
            for (Map.Entry<String, String> entry : sheet.getAssignedTo().entrySet()) {
                String type = sheet.getRecipientTypes().getOrDefault(entry.getKey(), "ACTION");
                t.append("  - ").append(entry.getValue()).append(" (FOR ").append(type).append(")\n");
            }
        }

        t.append("\n------------------------------------------\n");
        t.append("ACTION REQUIRED\n");
        t.append("Click a link below to respond:\n\n");

        String[] responses = {"ACTION TAKEN", "COMPLETED", "IN PROGRESS", "APPROVED", "NOTED", "NEEDS REVIEW", "REJECTED"};
        for (String r : responses) {
            String url = ResponseController.buildResponseUrl(baseUrl, sheet.getId(), toEmail, r);
            t.append(r).append(":\n").append(url).append("\n\n");
        }

        return t.toString();
    }

    /**
     * Send test email to verify configuration
     */
    public void sendTestEmail(String toEmail) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, "Al-Ahlia Contracting Group");
        helper.setTo(toEmail);
        helper.setSubject("Action Sheet System - Test Email");
        helper.setText(
                "This is a test email from the Action Sheet System.\n" +
                "If you received this, your email configuration is working correctly.",
                "<div style=\"font-family:'Segoe UI',sans-serif; max-width:500px; margin:0 auto; padding:20px;\">" +
                "<h2 style=\"color:#800000;\">Email Configuration Working</h2>" +
                "<p>This is a test email from the <strong>Al-Ahlia Action Sheet System</strong>.</p>" +
                "<p style=\"color:#1a7a3a; font-weight:bold;\">Your email relay is configured correctly.</p>" +
                "<hr style=\"border:1px solid #eee;\"/>" +
                "<p style=\"color:#888; font-size:12px;\">Al-Ahlia Contracting Group</p></div>"
        );

        mailSender.send(message);
        log.info("Test email sent to: {}", toEmail);
    }
}
