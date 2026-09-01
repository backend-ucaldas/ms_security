package com.uc.ms_security.auth.dto;

import java.time.Instant;

public record AuthResponseDTO(
        String accessToken,
        String tokenType,
        Instant expiresAt) {
}
