package com.uc.ms_security.controller;

import com.uc.ms_security.dto.AuthResponseDTO;
import com.uc.ms_security.dto.LoginRequestDTO;
import com.uc.ms_security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDTO login(
            @Valid @RequestBody LoginRequestDTO dto) {

        return authService.login(dto);
    }
}