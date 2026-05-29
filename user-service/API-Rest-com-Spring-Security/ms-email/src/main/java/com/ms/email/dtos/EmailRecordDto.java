package com.ms.email.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record EmailRecordDto(
        UUID userId,
        @NotBlank @Email String emailTo,
        @NotBlank String subject,
        @NotBlank String text
) {}
