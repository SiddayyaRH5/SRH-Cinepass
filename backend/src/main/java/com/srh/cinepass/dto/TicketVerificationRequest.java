package com.srh.cinepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketVerificationRequest(
        @NotBlank(message = "Verification token is required")
        @Size(max = 256, message = "Verification token is invalid")
        String verificationToken
) {
}
