package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class PermissionRoleResponseDTO {

    Long id;

    Long permissionId;

    RoleResponseDTO role;
}
