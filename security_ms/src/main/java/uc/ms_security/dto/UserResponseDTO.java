package uc.security_ms.dto;
import lombok.Value;

@Value
public class UserResponseDTO {
    Long id;
    String name;
    String email;
}