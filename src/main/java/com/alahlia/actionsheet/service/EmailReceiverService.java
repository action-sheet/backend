package com.alahlia.actionsheet.service;

import com.alahlia.actionsheet.entity.ActionSheet;
import jakarta.mail.*;
import jakarta.mail.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email receiver service with IMAP polling
 * Ported from legacy EmailReceiver.java
 * Polls for email responses and updates action sheets
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.email.receiver.enabled", havingValue = "true")
public class EmailReceiverService {

    private final ActionSheetService actionSheetService;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${app.email.imap.host:imap.gmail.com}")
    private String imapHost;

    @Value("${app.email.imap.port:993}")
    private String imapPort;

    private Store store;
    private Folder inbox;
    private final Set<String> processedMessageIds = Collections.synchronizedSet(new HashSet<>());

    // Patterns for extracting action sheet ID
    private static final Pattern REF_ID_PATTERN = Pattern.compile("\\[Ref:([A-Za-z0-9_-]+)\\]");
    private static final Pattern FLEXIBLE_ID_PATTERN = Pattern.compile("([A-Za-z0-9]+-\\d{6}-\\d{8})");

    /**
     * Poll for new emails every 30 seconds
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void checkForNewEmails() {
        try {
            if (!isConnected()) {
                connect();
            }

            if (inbox == null || !inbox.isOpen()) {
                return;
            }

            // Search for unseen messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            
            if (messages.length > 0) {
                log.info("Found {} new email(s)", messages.length);
                
                for (Message message : messages) {
                    try {
                        processEmail(message);
                        message.setFlag(Flags.Flag.SEEN, true);
                    } catch (Exception e) {
                        log.error("Error processing email", e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error checking emails", e);
            disconnect();
        }
    }

    /**
     * Process a single email message
     */
    private void processEmail(Message message) throws Exception {
        String subject = message.getSubject();
        String from = getFromAddress(message);
        String content = getEmailContent(message);

        // Get message ID for deduplication
        String messageId = getMessageId(message);
        if (messageId != null && processedMessageIds.contains(messageId)) {
            log.debug("Duplicate message ignored: {}", messageId);
            return;
        }

        log.info("Processing email from: {} | Subject: {}", from, subject);

        // Extract action sheet ID
        String actionSheetId = extractActionSheetId(subject, content);
        if (actionSheetId == null) {
            log.debug("Not an action sheet response: {}", subject);
            return;
        }

        // Get action sheet
        ActionSheet sheet = actionSheetService.getActionSheetEntity(actionSheetId);
        if (sheet == null) {
            log.warn("Action sheet not found: {}", actionSheetId);
            return;
        }

        // Find matching email (case-insensitive)
        String matchedEmail = findMatchingEmail(sheet, from);
        if (matchedEmail == null) {
            log.warn("Email not in recipients list: {}", from);
            matchedEmail = from; // Use anyway
        }

        // Check if INFO-only recipient
        String recipientType = sheet.getRecipientTypes().get(matchedEmail);
        if ("INFO".equalsIgnoreCase(recipientType)) {
            log.info("INFO-only recipient reply ignored: {}", matchedEmail);
            return;
        }

        // Extract response keyword
        String response = extractResponse(content);
        if (response == null) {
            log.warn("No valid response keyword found from: {}", from);
            return;
        }

        // Check if already responded
        if (sheet.getResponses().containsKey(matchedEmail)) {
            log.debug("Response already recorded for {}", matchedEmail);
            return;
        }

        // Add response
        actionSheetService.addResponse(actionSheetId, matchedEmail, response, 
                                      matchedEmail, null, 5);

        // Mark as processed
        if (messageId != null) {
            processedMessageIds.add(messageId);
        }

        log.info("✅ Response recorded - Sheet: {} | From: {} | Response: {}", 
                actionSheetId, matchedEmail, response);
    }

    /**
     * Extract action sheet ID from subject or content
     */
    private String extractActionSheetId(String subject, String content) {
        // Try [Ref:ID] pattern first
        if (subject != null) {
            Matcher m = REF_ID_PATTERN.matcher(subject);
            if (m.find()) {
                return m.group(1);
            }
        }

        // Try flexible pattern
        if (subject != null) {
            Matcher m = FLEXIBLE_ID_PATTERN.matcher(subject);
            if (m.find()) {
                return m.group(1);
            }
        }

        // Try in content
        if (content != null) {
            Matcher m = REF_ID_PATTERN.matcher(content);
            if (m.find()) {
                return m.group(1);
            }
            m = FLEXIBLE_ID_PATTERN.matcher(content);
            if (m.find()) {
                return m.group(1);
            }
        }

        return null;
    }

    /**
     * Extract response keyword from email content
     */
    private String extractResponse(String content) {
        if (content == null) return null;

        String upper = content.toUpperCase();

        // Priority order
        if (upper.contains("ACTION TAKEN")) return "ACTION TAKEN";
        if (upper.contains("COMPLETED")) return "COMPLETED";
        if (upper.contains("DONE")) return "DONE";
        if (upper.contains("FINISHED")) return "FINISHED";
        if (upper.contains("APPROVED")) return "APPROVED";
        if (upper.contains("ACCEPTED")) return "ACCEPTED";
        if (upper.contains("REJECTED")) return "REJECTED";
        if (upper.contains("DECLINED")) return "DECLINED";
        if (upper.contains("IN PROGRESS")) return "IN PROGRESS";
        if (upper.contains("WORKING")) return "WORKING";
        if (upper.contains("NEEDS REVIEW")) return "NEEDS REVIEW";
        if (upper.contains("REVIEW")) return "REVIEW";
        if (upper.contains("NOTED")) return "NOTED";
        if (upper.contains("ACKNOWLEDGED")) return "ACKNOWLEDGED";

        return null;
    }

    /**
     * Find matching email in action sheet (case-insensitive)
     */
    private String findMatchingEmail(ActionSheet sheet, String from) {
        String fromLower = from.toLowerCase();
        
        for (String email : sheet.getAssignedTo().keySet()) {
            if (email.toLowerCase().equals(fromLower)) {
                return email;
            }
        }
        
        return null;
    }

    /**
     * Get email content as text
     */
    private String getEmailContent(Message message) throws Exception {
        Object content = message.getContent();
        
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    sb.append(bodyPart.getContent().toString());
                } else if (bodyPart.isMimeType("text/html")) {
                    sb.append(bodyPart.getContent().toString());
                }
            }
            
            return sb.toString();
        }
        
        return "";
    }

    /**
     * Get from address
     */
    private String getFromAddress(Message message) throws Exception {
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            return from[0].toString();
        }
        return "";
    }

    /**
     * Get message ID
     */
    private String getMessageId(Message message) {
        try {
            String[] ids = message.getHeader("Message-ID");
            if (ids != null && ids.length > 0) {
                return ids[0];
            }
        } catch (Exception e) {
            log.debug("Could not get Message-ID", e);
        }
        return null;
    }

    /**
     * Connect to IMAP server
     */
    private void connect() throws Exception {
        Properties props = new Properties();
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", imapPort);
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.auth", "true");
        props.put("mail.imap.connectiontimeout", "15000");
        props.put("mail.imap.timeout", "30000");

        Session session = Session.getInstance(props);
        store = session.getStore("imap");
        store.connect(username, password);

        inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        log.info("Connected to IMAP server: {}", imapHost);
    }

    /**
     * Disconnect from IMAP server
     */
    private void disconnect() {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
        } catch (Exception e) {
            log.debug("Error closing inbox", e);
        }
        
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (Exception e) {
            log.debug("Error closing store", e);
        }
        
        inbox = null;
        store = null;
    }

    /**
     * Check if connected
     */
    private boolean isConnected() {
        return store != null && store.isConnected() && 
               inbox != null && inbox.isOpen();
    }
}
