package com.ms.userservice.dtos;

import com.ms.userservice.entities.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileDto(
    @NotBlank String name,
    @NotNull RoleName role
) {}
