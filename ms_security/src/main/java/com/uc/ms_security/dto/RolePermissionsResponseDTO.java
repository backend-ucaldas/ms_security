package com.uc.ms_security.dto;

import lombok.Value;

import java.util.List;

@Value
public class RolePermissionsResponseDTO {

    Long id;

    String name;

    String description;

    List<RolePermissionResponseDTO> permissions;
}
