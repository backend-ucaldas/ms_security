package com.uc.ms_security.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO extends BaseUserDTO {

    @Size(  min = 8,
        max = 72,
        message = "La contraseña debe tener entre 8 y 72 caracteres"
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])\\S{8,72}$",
        message = "La contraseña debe contener al menos una mayúscula, "
            + "una minúscula, un número y un carácter especial, "
            + "sin espacios"
    )
    private String password;
}