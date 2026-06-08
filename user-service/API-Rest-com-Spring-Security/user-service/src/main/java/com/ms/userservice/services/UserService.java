package com.ms.userservice.services;

import com.ms.userservice.dtos.CreateUserDto;
import com.ms.userservice.dtos.LoginUserDto;
import com.ms.userservice.dtos.RecoveryJwtTokenDto;
import com.ms.userservice.entities.Role;
import com.ms.userservice.entities.RoleName;
import com.ms.userservice.entities.User;
import com.ms.userservice.repositories.RoleRepository;
import com.ms.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public RecoveryJwtTokenDto authenticateUser(LoginUserDto dto) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(dto.email(), dto.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return new RecoveryJwtTokenDto(jwtTokenService.generateToken(userDetails));
    }

    public void createUser(CreateUserDto dto) {
        RoleName roleName = dto.role() != null ? dto.role() : RoleName.ROLE_CUSTOMER;
        Role role = roleRepository.findByName(roleName)
            .orElseGet(() -> roleRepository.save(new Role(roleName)));
        User user = new User(dto.email(), passwordEncoder.encode(dto.password()), List.of(role));
        userRepository.save(user);
    }

    public UUID encontrarOuCriarUsuario(String email) {
        return userRepository.findByEmail(email)
            .map(u -> UUID.nameUUIDFromBytes(String.valueOf(u.getId()).getBytes()))
            .orElseGet(() -> {
                Role role = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_CUSTOMER)));
                String senhaAleatoria = UUID.randomUUID().toString();
                User novoUsuario = new User(email, passwordEncoder.encode(senhaAleatoria), List.of(role));
                userRepository.save(novoUsuario);
                return UUID.nameUUIDFromBytes(String.valueOf(novoUsuario.getId()).getBytes());
            });
    }
}
