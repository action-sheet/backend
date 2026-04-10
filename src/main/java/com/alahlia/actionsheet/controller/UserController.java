package com.alahlia.actionsheet.controller;

import com.alahlia.actionsheet.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin user management — CRUD for users.json.
 * Passwords are stored AES-encrypted. Plain-text passwords are encrypted before storage.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> safe = userService.getAllUsers().stream().map(u -> Map.<String, Object>of(
                "username", u.getUsername() != null ? u.getUsername() : "",
                "email", u.getEmail() != null ? u.getEmail() : "",
                "role", u.getRole() != null ? u.getRole() : "user",
                "createdTimestamp", u.getCreatedTimestamp(),
                "lastLogin", u.getLastLogin()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(safe);
    }

    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String role = body.getOrDefault("role", "user");

        if (username == null || username.isBlank() || email == null || email.isBlank() || password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username, email and password are required"));
        }

        boolean added = userService.addUser(username.trim(), email.trim(), password, role.trim());
        if (!added) {
            return ResponseEntity.badRequest().body(Map.of("error", "User with this email already exists"));
        }
        log.info("Admin added user: {} ({})", username, email);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateUser(@PathVariable String email, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        String newUsername = body.get("username");
        String newPassword = body.get("password");

        boolean updated = userService.updateUser(email, newUsername, newRole, newPassword);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        log.info("Admin updated user: {}", email);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        boolean deleted = userService.deleteUser(email);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        log.info("Admin deleted user: {}", email);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
