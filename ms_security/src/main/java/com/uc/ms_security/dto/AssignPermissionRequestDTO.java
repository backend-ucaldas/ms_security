package com.uc.ms_security.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignPermissionRequestDTO {

    @NotNull(message = "El identificador del rol es obligatorio")
    private Long roleId;

    @NotNull(message = "El identificador del permiso es obligatorio")
    private Long permissionId;
}
