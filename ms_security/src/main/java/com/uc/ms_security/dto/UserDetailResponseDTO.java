package com.uc.ms_security.dto;

import lombok.Value;

@Value
public class UserDetailResponseDTO {

    Long id;

    String name;

    String email;

    ProfileResponseDTO profile;
}