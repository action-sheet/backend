package com.alahlia.actionsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Conflict discussion thread message.
 */
@Entity
@Table(name = "conflict_threads")
@Getter
@Setter
@NoArgsConstructor
public class ConflictThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_sheet_id", nullable = false)
    private ActionSheet actionSheet;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String message;

    public ConflictThread(LocalDateTime timestamp, String author, String message) {
        this.timestamp = timestamp;
        this.author = author;
        this.message = message;
    }
}
