package com.alahlia.actionsheet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Project entity - migrated from ProjectManager.Project
 */
@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
public class Project {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(length = 1000)
    private String path;

    @Column(nullable = false)
    private boolean active = true;

    public Project(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdDate = LocalDateTime.now();
    }
}
