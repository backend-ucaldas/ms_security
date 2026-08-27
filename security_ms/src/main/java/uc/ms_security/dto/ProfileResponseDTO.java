package uc.security_ms.dto;

import lombok.Value;

import java.time.LocalDate;

@Value
public class ProfileResponseDTO {
    Long id;
    String phone;
    LocalDate birthDate;
}
