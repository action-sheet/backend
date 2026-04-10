package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.entity.Employee;
import com.alahlia.actionsheet.service.EmployeeService;
import com.alahlia.actionsheet.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller.
 * Authenticates against users.json (AES-encrypted passwords).
 * Enriches response with employee directory data when available.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

    private final EmployeeService employeeService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        // Authenticate against users.json
        UserService.User authUser = userService.authenticate(email.trim(), password);
        if (authUser == null) {
            log.warn("Login failed for: {}", email);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        // Enrich with employee directory data if available
        Employee employee = null;
        try {
            String lookupEmail = authUser.getEmail() != null ? authUser.getEmail() : email;
            employee = employeeService.getEmployee(lookupEmail);
        } catch (Exception e) {
            log.debug("No employee record found for {}", email);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", authUser.getEmail() != null ? authUser.getEmail() : email);
        response.put("name", employee != null ? employee.getName() : authUser.getUsername());
        response.put("role", authUser.getRole() != null ? authUser.getRole() : "user");
        response.put("department", employee != null ? employee.getDepartment() : "General");
        response.put("hierarchyLevel", employee != null ? employee.getHierarchyLevel() : 99);

        log.info("User {} logged in successfully (role: {})", email, authUser.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UserService.User authUser = userService.getUserByEmail(email.trim());
        if (authUser == null) {
            return ResponseEntity.status(401).build();
        }

        Employee employee = null;
        try {
            String lookupEmail = authUser.getEmail() != null ? authUser.getEmail() : email;
            employee = employeeService.getEmployee(lookupEmail);
        } catch (Exception ignored) {}

        Map<String, Object> response = new HashMap<>();
        response.put("email", authUser.getEmail() != null ? authUser.getEmail() : email);
        response.put("name", employee != null ? employee.getName() : authUser.getUsername());
        response.put("role", authUser.getRole() != null ? authUser.getRole() : "user");
        response.put("department", employee != null ? employee.getDepartment() : "General");
        response.put("hierarchyLevel", employee != null ? employee.getHierarchyLevel() : 99);

        return ResponseEntity.ok(response);
    }
}
