package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class RoleUserResponseDTO {

    Long id;

    Long roleId;

    UserResponseDTO user;
}
