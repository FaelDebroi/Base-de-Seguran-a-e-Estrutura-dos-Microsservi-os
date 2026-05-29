package com.ms.userservice.dtos;

import com.ms.userservice.entities.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserDto(
    @NotBlank @Email String email,
    @NotBlank String password,
    RoleName role
) {}
