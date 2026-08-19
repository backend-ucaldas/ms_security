package uc.security_ms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseUserDTO {

    @NotBlank(
            message = "El nombre es obligatorio"
    )
    @Size(
            min = 2,
            max = 100,
            message = "El nombre debe tener entre 2 y 100 caracteres"
    )
    private String name;

    @NotBlank(
            message = "El email es obligatorio"
    )
    @Email(
            message = "El email no tiene un formato válido"
    )
    private String email;
}
