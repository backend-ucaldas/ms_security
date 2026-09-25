package com.uc.ms_security.service;

import com.uc.ms_security.dto.AuthResponseDTO;
import com.uc.ms_security.dto.LoginRequestDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(this::unauthorized);

        boolean validPassword = passwordEncoder.matches(
                dto.getPassword(),
                user.getPassword()
        );

        if (!validPassword) {
            throw unauthorized();
        }

        return jwtService.generateToken(user);
    }

    private ApplicationException unauthorized() {
        return new ApplicationException(
                ErrorCase.UNAUTHORIZED,
                "Credenciales incorrectas"
        );
    }
}