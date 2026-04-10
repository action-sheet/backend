package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.entity.Employee;
import com.alahlia.actionsheet.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller
 * Simple email-based authentication for now
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final EmployeeService employeeService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        Employee employee = employeeService.getEmployee(email);
        
        if (employee == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        if (!employee.isActive()) {
            return ResponseEntity.status(401).body(Map.of("error", "User is inactive"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", employee.getEmail());
        response.put("name", employee.getName());
        response.put("role", employee.getRole());
        response.put("department", employee.getDepartment());
        response.put("hierarchyLevel", employee.getHierarchyLevel());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestParam String email) {
        Employee employee = employeeService.getEmployee(email);
        
        if (employee == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", employee.getEmail());
        response.put("name", employee.getName());
        response.put("role", employee.getRole());
        response.put("department", employee.getDepartment());
        response.put("hierarchyLevel", employee.getHierarchyLevel());

        return ResponseEntity.ok(response);
    }
}
