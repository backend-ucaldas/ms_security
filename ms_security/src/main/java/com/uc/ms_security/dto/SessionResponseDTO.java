package com.uc.ms_security.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class SessionResponseDTO {

    Long id;

    String token;

    LocalDateTime expiration;

    String code2FA;
}