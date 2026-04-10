package com.alahlia.actionsheet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Validated request DTO for recording a response to an action sheet.
 */
@Data
public class RespondRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Response is required")
    private String response;

    private String senderUserId;
    private String senderRole;
    private Integer hierarchyLevel;
}
