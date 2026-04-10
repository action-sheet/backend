package com.alahlia.actionsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Conflict event for audit trail.
 */
@Entity
@Table(name = "conflict_events")
@Getter
@Setter
@NoArgsConstructor
public class ConflictEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_sheet_id", nullable = false)
    private ActionSheet actionSheet;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String resolvedBy;

    private LocalDateTime resolvedAt;

    public ConflictEvent(LocalDateTime timestamp, String severity, String description) {
        this.timestamp = timestamp;
        this.severity = severity;
        this.description = description;
    }
}
