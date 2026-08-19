package uc.security_ms.dto;


import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO extends BaseUserDTO {

    @Size(  min = 8,
            message = "La contraseña debe tener mínimo 8 caracteres"
    )
    private String password;
}
