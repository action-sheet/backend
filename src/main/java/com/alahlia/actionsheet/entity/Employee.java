package com.alahlia.actionsheet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Employee entity - migrated from DataStorage.Employee
 */
@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
public class Employee {

    @Id
    @Column(length = 200)
    private String email;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String department;

    @Column(length = 100)
    private String position;

    @Column(length = 50)
    private String role; // GM, Admin, User, Viewer

    private Integer hierarchyLevel = 5; // Default level

    @Column(length = 200)
    private String bossEmail;

    @Column(length = 200)
    private String activeDirectory;

    @Column(length = 200)
    private String adObjectGuid;

    @Column(length = 500)
    private String adDistinguishedName;

    private LocalDateTime lastAdSyncTime;

    private boolean adSynced = false;

    @Column(name = "is_group")
    private boolean group = false;

    @Column(nullable = false)
    private boolean active = true;

    public Employee(String email, String name) {
        this.email = email;
        this.name = name;
    }
}
