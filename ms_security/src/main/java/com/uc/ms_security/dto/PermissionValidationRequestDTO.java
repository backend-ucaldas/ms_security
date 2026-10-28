package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionValidationRequestDTO {

    @NotBlank(message = "La url es obligatoria")
    @Size(max = 255, message = "La url no puede superar 255 caracteres")
    private String url;

    @NotBlank(message = "El método es obligatorio")
    @Pattern(
            regexp = "GET|POST|PUT|PATCH|DELETE",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "El método HTTP no es válido"
    )
    private String method;
}
