package com.uc.ms_security.dto;

import lombok.Value;

import java.util.List;

@Value
public class UserRolesResponseDTO {

    Long id;

    String name;

    String email;

    List<UserRoleResponseDTO> roles;
}
