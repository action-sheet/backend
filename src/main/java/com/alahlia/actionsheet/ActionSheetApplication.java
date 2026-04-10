package com.alahlia.actionsheet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for Al-Ahlia Action Sheet Management System.
 * Replaces the legacy Main.java Swing application.
 */
@SpringBootApplication
@EnableScheduling
public class ActionSheetApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActionSheetApplication.class, args);
    }
}
