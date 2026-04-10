package com.alahlia.actionsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Validated request DTO for creating/updating action sheets.
 */
@Data
public class CreateSheetRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be at most 500 characters")
    private String title;

    /** Optional — defaults to 7 days from now if not set */
    private LocalDateTime dueDate;

    /** Optional status to set (e.g. DRAFT, PENDING) */
    private String status;

    @Size(max = 50, message = "Project ID must be at most 50 characters")
    private String projectId;

    private Map<String, String> assignedTo = new HashMap<>();
    private Map<String, String> recipientTypes = new HashMap<>();
    private Map<String, Object> formData = new HashMap<>();
}
