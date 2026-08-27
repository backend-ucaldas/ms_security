package uc.security_ms.dto;

import lombok.Value;

@Value
public class UserDetailResponseDTO {
    Long id;
    String name;
    String email;
    ProfileResponseDTO profile;
}
