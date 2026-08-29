package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.entity.Role;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleMapper {

    public Role toEntity(RoleRequestDTO dto) {
        Role role = new Role();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        return role;
    }

    public void updateEntity(
            RoleRequestDTO dto,
            Role role) {

        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
    }

    public RoleResponseDTO toResponseDTO(Role role) {
        return new RoleResponseDTO(
                role.getId(),
                role.getName(),
                role.getDescription()
        );
    }

    public List<RoleResponseDTO> toResponseDTOList(
            List<Role> roles) {

        return roles.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
