package com.ms.userservice.controllers;

import com.ms.userservice.dtos.CreateUserDto;
import com.ms.userservice.dtos.LoginUserDto;
import com.ms.userservice.dtos.RecoveryJwtTokenDto;
import com.ms.userservice.dtos.UpdateProfileDto;
import com.ms.userservice.dtos.UserProfileDto;
import com.ms.userservice.entities.User;
import com.ms.userservice.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserDto dto) {
        userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<RecoveryJwtTokenDto> login(@Valid @RequestBody LoginUserDto dto) {
        RecoveryJwtTokenDto token = userService.authenticateUser(dto);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/update-profile")
    public ResponseEntity<UserProfileDto> updateProfile(Authentication authentication,
                                                        @Valid @RequestBody UpdateProfileDto dto) {
        String email = authentication.getName();
        User updated = userService.updateProfile(email, dto);
        List<String> roleNames = updated.getRoles().stream()
            .map(r -> r.getName().name())
            .collect(Collectors.toList());
        UserProfileDto profile = new UserProfileDto(updated.getEmail(), updated.getName(), roleNames);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(Authentication authentication) {
        String email = authentication.getName();
        UserProfileDto profile = userService.getProfile(email);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/test/customer")
    public ResponseEntity<String> testCustomer() {
        return ResponseEntity.ok("Acesso liberado para role CUSTOMER");
    }

    @GetMapping("/test/administrator")
    public ResponseEntity<String> testAdministrator() {
        return ResponseEntity.ok("Acesso liberado para role ADMINISTRATOR");
    }
}
