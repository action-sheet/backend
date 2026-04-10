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
    private final com.alahlia.actionsheet.service.UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        // Authenticate against users.json securely
        com.alahlia.actionsheet.service.UserService.User authUser = userService.authenticate(email, password);
        if (authUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        // Optional: Get department and hierarchy from employee records if available
        Employee employee = employeeService.getEmployee(email);

        Map<String, Object> response = new HashMap<>();
        // Fallback to authUser's email and role since it's the primary authenticator 
        response.put("email", authUser.getEmail() != null ? authUser.getEmail() : email);
        response.put("name", employee != null ? employee.getName() : authUser.getUsername());
        response.put("role", authUser.getRole()); // role comes from users.json securely
        response.put("department", employee != null ? employee.getDepartment() : "General");
        response.put("hierarchyLevel", employee != null ? employee.getHierarchyLevel() : 99);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestParam String email) {
        com.alahlia.actionsheet.service.UserService.User authUser = userService.getUserByEmail(email);
        
        if (authUser == null) {
            return ResponseEntity.status(401).build();
        }

        Employee employee = employeeService.getEmployee(email);

        Map<String, Object> response = new HashMap<>();
        response.put("email", authUser.getEmail() != null ? authUser.getEmail() : email);
        response.put("name", employee != null ? employee.getName() : authUser.getUsername());
        response.put("role", authUser.getRole());
        response.put("department", employee != null ? employee.getDepartment() : "General");
        response.put("hierarchyLevel", employee != null ? employee.getHierarchyLevel() : 99);

        return ResponseEntity.ok(response);
    }
}
