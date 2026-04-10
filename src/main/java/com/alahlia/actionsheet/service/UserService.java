package com.alahlia.actionsheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class UserService {

    @Value("${files.path:Z:/Action Sheet System/data}")
    private String dataPath;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<User> users = new CopyOnWriteArrayList<>();
    private File usersFile;

    // Same AES-128 key as the legacy UserManager
    private static final byte[] AES_KEY = "A1h@L1a$hEEt2026".getBytes();

    @Data
    public static class User {
        private String username;
        private String email;
        private String password;
        private String role;
        private long createdTimestamp;
        private long lastLogin;
    }

    @PostConstruct
    public void init() {
        usersFile = new File(dataPath, "users.json");
        log.info("UserService: users.json location: {}", usersFile.getAbsolutePath());
        loadUsers();
    }

    private synchronized void loadUsers() {
        if (!usersFile.exists()) {
            log.warn("users.json not found at {}", usersFile.getAbsolutePath());
            return;
        }
        try {
            List<User> loaded = mapper.readValue(usersFile, new TypeReference<List<User>>() {});
            users.clear();
            users.addAll(loaded);
            log.info("Loaded {} users from users.json", users.size());
        } catch (Exception e) {
            log.error("Failed to load users: {}", e.getMessage(), e);
        }
    }

    private synchronized void saveUsers() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(usersFile, users);
            log.info("Saved {} users to users.json", users.size());
        } catch (Exception e) {
            log.error("Failed to save users: {}", e.getMessage(), e);
        }
    }

    // ==================== ENCRYPTION ====================

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        try {
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            return plainText;
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        try {
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            // Might be plain text from old format — return as-is
            return encryptedText;
        }
    }

    // ==================== AUTHENTICATION ====================

    /**
     * Authenticate by email OR username + plain password.
     * Refreshes from disk first to pick up changes from other machines.
     */
    public User authenticate(String loginId, String plainPassword) {
        if (loginId == null || loginId.isBlank() || plainPassword == null || plainPassword.isEmpty()) {
            log.warn("UserService: authenticate called with null/blank credentials");
            return null;
        }

        loadUsers(); // refresh from shared drive

        String loginTrimmed = loginId.trim();

        // Find user by email OR username (null-safe)
        User user = users.stream()
                .filter(u -> loginTrimmed.equalsIgnoreCase(u.getEmail())
                          || loginTrimmed.equalsIgnoreCase(u.getUsername()))
                .findFirst()
                .orElse(null);

        if (user == null) {
            log.warn("UserService: Login failed — user not found: {}", loginTrimmed);
            return null;
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isEmpty()) {
            log.warn("UserService: User {} has no password set", loginTrimmed);
            return null;
        }

        // Try AES decrypt and compare
        String decrypted = decrypt(storedPassword);
        if (plainPassword.equals(decrypted)) {
            user.setLastLogin(System.currentTimeMillis());
            saveUsers();
            log.info("UserService: User {} authenticated successfully", loginTrimmed);
            return user;
        }

        // Fallback: direct comparison (legacy plain-text passwords)
        if (plainPassword.equals(storedPassword)) {
            // Migrate: re-encrypt the password
            user.setPassword(encrypt(plainPassword));
            user.setLastLogin(System.currentTimeMillis());
            saveUsers();
            log.info("UserService: User {} authenticated (migrated to encrypted)", loginTrimmed);
            return user;
        }

        log.warn("UserService: Login failed — wrong password for: {}", loginTrimmed);
        return null;
    }

    // ==================== USER QUERIES ====================

    /**
     * Get all users (returns defensive copy, never exposes internal list)
     */
    public List<User> getAllUsers() {
        loadUsers(); // always refresh from disk
        return Collections.unmodifiableList(new ArrayList<>(users));
    }

    /**
     * Find user by email OR username (null-safe)
     */
    public User getUserByEmail(String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        String id = identifier.trim();
        return users.stream()
                .filter(u -> id.equalsIgnoreCase(u.getEmail())
                          || id.equalsIgnoreCase(u.getUsername()))
                .findFirst()
                .orElse(null);
    }

    // ==================== USER CRUD ====================

    /**
     * Add a new user. Password is encrypted before storage.
     * Returns false if a user with the same email already exists.
     */
    public boolean addUser(User newUser) {
        if (newUser == null || newUser.getEmail() == null || newUser.getEmail().isBlank()) {
            log.warn("UserService: addUser called with invalid data");
            return false;
        }

        // Check duplicate by email
        if (getUserByEmail(newUser.getEmail()) != null) {
            log.warn("UserService: Duplicate user email: {}", newUser.getEmail());
            return false;
        }

        newUser.setPassword(encrypt(newUser.getPassword()));
        newUser.setCreatedTimestamp(System.currentTimeMillis());
        users.add(newUser);
        saveUsers();
        log.info("UserService: Added user {} ({})", newUser.getUsername(), newUser.getRole());
        return true;
    }

    /**
     * Update an existing user. If password is provided, it is encrypted before storage.
     */
    public boolean updateUser(String email, User updated) {
        if (email == null || email.isBlank() || updated == null) return false;

        User user = getUserByEmail(email);
        if (user == null) {
            log.warn("UserService: updateUser — user not found: {}", email);
            return false;
        }

        if (updated.getUsername() != null && !updated.getUsername().isBlank()) {
            user.setUsername(updated.getUsername());
        }
        if (updated.getRole() != null && !updated.getRole().isBlank()) {
            user.setRole(updated.getRole());
        }
        if (updated.getPassword() != null && !updated.getPassword().isEmpty()) {
            user.setPassword(encrypt(updated.getPassword()));
        }
        if (updated.getEmail() != null && !updated.getEmail().isBlank()
                && !updated.getEmail().equalsIgnoreCase(email)) {
            user.setEmail(updated.getEmail());
        }
        saveUsers();
        log.info("UserService: Updated user {}", email);
        return true;
    }

    /**
     * Delete a user by email.
     */
    public boolean deleteUser(String email) {
        if (email == null || email.isBlank()) return false;
        boolean removed = users.removeIf(u ->
                email.equalsIgnoreCase(u.getEmail()));
        if (removed) {
            saveUsers();
            log.info("UserService: Removed user {}", email);
        }
        return removed;
    }

    /** Convenience: add user from plain parameters. */
    public boolean addUser(String username, String email, String plainPassword, String role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(plainPassword);
        u.setRole(role);
        return addUser(u);
    }

    /** Convenience: update user from plain parameters. */
    public boolean updateUser(String email, String newUsername, String newRole, String newPassword) {
        User updated = new User();
        updated.setUsername(newUsername);
        updated.setRole(newRole);
        updated.setPassword(newPassword);
        return updateUser(email, updated);
    }
}
