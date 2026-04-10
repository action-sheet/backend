package com.alahlia.actionsheet.websocket;

import com.alahlia.actionsheet.entity.ActionSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes WebSocket events for real-time dashboard updates
 * Replaces NetworkManager broadcast functionality
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishSheetCreated(ActionSheet sheet) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "SHEET_CREATED");
        event.put("sheetId", sheet.getId());
        event.put("sheet", sheet);
        
        messagingTemplate.convertAndSend("/topic/sheets", event);
        log.debug("Published SHEET_CREATED event for: {}", sheet.getId());
    }

    public void publishSheetUpdated(ActionSheet sheet) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "SHEET_UPDATED");
        event.put("sheetId", sheet.getId());
        event.put("sheet", sheet);
        
        messagingTemplate.convertAndSend("/topic/sheets", event);
        log.debug("Published SHEET_UPDATED event for: {}", sheet.getId());
    }

    public void publishSheetDeleted(String sheetId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "SHEET_DELETED");
        event.put("sheetId", sheetId);
        
        messagingTemplate.convertAndSend("/topic/sheets", event);
        log.debug("Published SHEET_DELETED event for: {}", sheetId);
    }

    public void publishResponseAdded(String sheetId, String email, String response) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "RESPONSE_ADDED");
        event.put("sheetId", sheetId);
        event.put("email", email);
        event.put("response", response);
        
        messagingTemplate.convertAndSend("/topic/sheets", event);
        log.debug("Published RESPONSE_ADDED event for sheet: {}", sheetId);
    }

    public void publishStatusChanged(String sheetId, String oldStatus, String newStatus) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "STATUS_CHANGED");
        event.put("sheetId", sheetId);
        event.put("oldStatus", oldStatus);
        event.put("newStatus", newStatus);
        
        messagingTemplate.convertAndSend("/topic/sheets", event);
        log.debug("Published STATUS_CHANGED event for sheet: {} ({} -> {})", 
                sheetId, oldStatus, newStatus);
    }
}
