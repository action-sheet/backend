package com.alahlia.actionsheet;

import com.alahlia.actionsheet.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for Al-Ahlia Action Sheet Management System.
 * Replaces the legacy Main.java Swing application.
 */
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class ActionSheetApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActionSheetApplication.class, args);
    }

    @Bean
    public CommandLineRunner initializeAttachments(AttachmentService attachmentService) {
        return args -> attachmentService.ensureAttachmentsDirectory();
    }
}
