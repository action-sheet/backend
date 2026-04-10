package com.alahlia.actionsheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class UserService {

    @Value("${files.path:Z:/Action Sheet System/data}")
    private String dataPath;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<User> users = new CopyOnWriteArrayList<>();
    private File usersFile;

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
            log.error("Failed to load users: {}", e.getMessage());
        }
    }

    private synchronized void saveUsers() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(usersFile, users);
        } catch (Exception e) {
            log.error("Failed to save users: {}", e.getMessage());
        }
    }

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
            return encryptedText;
        }
    }

    public User authenticate(String email, String plainPassword) {
        loadUsers(); // refresh from disk
        User user = users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email) || u.getUsername().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);

        if (user == null) return null;

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isEmpty()) return null;

        String decrypted = decrypt(storedPassword);
        if (plainPassword.equals(decrypted)) {
            user.setLastLogin(System.currentTimeMillis());
            saveUsers();
            return user;
        }
        
        // legacy check
        if (plainPassword.equals(storedPassword)) {
            user.setPassword(encrypt(plainPassword));
            user.setLastLogin(System.currentTimeMillis());
            saveUsers();
            return user;
        }
        return null;
    }

    public List<User> getAllUsers() {
        return users;
    }

    public User getUserByEmail(String email) {
        return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
    }

    public void addUser(User user) {
        user.setPassword(encrypt(user.getPassword()));
        user.setCreatedTimestamp(System.currentTimeMillis());
        users.add(user);
        saveUsers();
    }

    public void updateUser(String email, User updated) {
        User user = getUserByEmail(email);
        if (user != null) {
            if (updated.getUsername() != null) user.setUsername(updated.getUsername());
            if (updated.getRole() != null) user.setRole(updated.getRole());
            if (updated.getPassword() != null && !updated.getPassword().isEmpty()) {
                user.setPassword(encrypt(updated.getPassword()));
            }
            saveUsers();
        }
    }

    public void deleteUser(String email) {
        users.removeIf(u -> u.getEmail().equalsIgnoreCase(email));
        saveUsers();
    }
}
