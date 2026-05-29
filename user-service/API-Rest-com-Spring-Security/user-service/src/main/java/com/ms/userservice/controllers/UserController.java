package com.ms.userservice.controllers;

import com.ms.userservice.dtos.CreateUserDto;
import com.ms.userservice.dtos.LoginUserDto;
import com.ms.userservice.dtos.RecoveryJwtTokenDto;
import com.ms.userservice.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/test/customer")
    public ResponseEntity<String> testCustomer() {
        return ResponseEntity.ok("Acesso liberado para role CUSTOMER");
    }

    @GetMapping("/test/administrator")
    public ResponseEntity<String> testAdministrator() {
        return ResponseEntity.ok("Acesso liberado para role ADMINISTRATOR");
    }
}
