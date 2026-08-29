package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.RoleUserResponseDTO;
import com.uc.ms_security.dto.UserResponseDTO;
import com.uc.ms_security.dto.UserRoleResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRoleMapper {

    private final RoleMapper roleMapper;

    public UserRoleResponseDTO toResponseDTO(
            UserRole userRole) {

        return new UserRoleResponseDTO(
                userRole.getId(),
                userRole.getUser().getId(),
                roleMapper.toResponseDTO(
                        userRole.getRole()
                )
        );
    }

    public List<UserRoleResponseDTO> toResponseDTOList(
            List<UserRole> userRoles) {

        return userRoles.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public RoleUserResponseDTO toRoleUserResponseDTO(
            UserRole userRole) {

        User user = userRole.getUser();

        return new RoleUserResponseDTO(
                userRole.getId(),
                userRole.getRole().getId(),
                new UserResponseDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                )
        );
    }

    public List<RoleUserResponseDTO> toRoleUserResponseDTOList(
            List<UserRole> userRoles) {

        return userRoles.stream()
                .map(this::toRoleUserResponseDTO)
                .toList();
    }
}
