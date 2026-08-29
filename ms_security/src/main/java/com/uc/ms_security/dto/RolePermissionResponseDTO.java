package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class RolePermissionResponseDTO {

    Long id;

    Long roleId;

    PermissionResponseDTO permission;
}
