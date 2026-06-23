package com.ms.userservice.dtos;

import java.util.List;

public record UserProfileDto(
    String email,
    String name,
    List<String> roles
) {}
