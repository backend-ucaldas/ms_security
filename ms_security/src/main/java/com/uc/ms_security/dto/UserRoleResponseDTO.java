package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class UserRoleResponseDTO {

    Long id;

    Long userId;

    RoleResponseDTO role;
}
