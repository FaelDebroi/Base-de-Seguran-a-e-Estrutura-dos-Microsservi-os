package com.ms.userservice.controllers;

import com.ms.userservice.dtos.EmailDto;
import com.ms.userservice.dtos.RecoveryJwtTokenDto;
import com.ms.userservice.producers.UserProducer;
import com.ms.userservice.services.CodigoCacheService;
import com.ms.userservice.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private CodigoCacheService codigoCacheService;

    @Autowired
    private UserProducer userProducer;

    @Autowired
    private UserService userService;

    @PostMapping({"/request-code", "/requestcode"})
    public ResponseEntity<Void> requestCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        UUID userId = userService.encontrarOuCriarUsuario(email);

        String codigo = String.format("%06d", new Random().nextInt(1_000_000));
        codigoCacheService.salvar(email, codigo);

        EmailDto emailDto = new EmailDto(
            email,
            "Seu código de acesso",
            "Seu código de acesso é: " + codigo,
            userId
        );
        userProducer.publicarEmailOtp(emailDto);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        if (!codigoCacheService.validar(email, code)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Código inválido ou expirado"));
        }

        RecoveryJwtTokenDto token = userService.gerarTokenParaEmail(email);
        return ResponseEntity.ok(token);
    }
}
