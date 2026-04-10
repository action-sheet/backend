package com.alahlia.actionsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response history entry for audit trail.
 */
@Entity
@Table(name = "response_entries")
@Getter
@Setter
@NoArgsConstructor
public class ResponseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_sheet_id", nullable = false)
    private ActionSheet actionSheet;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 100)
    private String response;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean overwritten = false;

    @Column(columnDefinition = "TEXT")
    private String rawContent;

    @Column(length = 100)
    private String senderUserId;

    @Column(length = 50)
    private String senderRole;

    private Integer senderHierarchyLevel;

    public ResponseEntry(String email, String response, LocalDateTime timestamp, boolean overwritten,
                         String rawContent, String senderUserId, String senderRole, Integer senderHierarchyLevel) {
        this.email = email;
        this.response = response;
        this.timestamp = timestamp;
        this.overwritten = overwritten;
        this.rawContent = rawContent;
        this.senderUserId = senderUserId;
        this.senderRole = senderRole;
        this.senderHierarchyLevel = senderHierarchyLevel;
    }
}
