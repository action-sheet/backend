package com.alahlia.actionsheet.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for CORS verification
 */
@RestController
@RequestMapping("/api/test")
@Slf4j
public class CorsTestController {

    @GetMapping("/cors")
    public ResponseEntity<Map<String, Object>> testCors(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS is working!");
        response.put("origin", request.getHeader("Origin"));
        response.put("method", request.getMethod());
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("CORS test request from origin: {}", request.getHeader("Origin"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cors")
    public ResponseEntity<Map<String, Object>> testCorsPost(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS POST is working!");
        response.put("received", body);
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("CORS POST test successful");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/headers")
    public ResponseEntity<Map<String, String>> showHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> 
            headers.put(name, request.getHeader(name))
        );
        
        log.info("Request headers: {}", headers);
        return ResponseEntity.ok(headers);
    }
}
