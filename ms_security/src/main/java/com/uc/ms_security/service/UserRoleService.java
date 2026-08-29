package com.uc.ms_security.service;

import com.uc.ms_security.dto.AssignRoleRequestDTO;
import com.uc.ms_security.dto.RoleUserResponseDTO;
import com.uc.ms_security.dto.UserRoleResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.entity.UserRole;
import com.uc.ms_security.mapper.UserRoleMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleMapper userRoleMapper;

    @Transactional
    public UserRoleResponseDTO assign(
            AssignRoleRequestDTO dto) {

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Rol no encontrado"
                        )
                );

        if (userRoleRepository.existsByUserIdAndRoleId(
                dto.getUserId(),
                dto.getRoleId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario ya tiene asignado ese rol"
            );
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        return userRoleMapper.toResponseDTO(
                userRoleRepository.save(userRole)
        );
    }

    @Transactional(readOnly = true)
    public List<UserRoleResponseDTO> findAll() {
        return userRoleMapper.toResponseDTOList(
                userRoleRepository.findAll()
        );
    }

    @Transactional(readOnly = true)
    public List<UserRoleResponseDTO> findByUserId(Long userId) {
        return userRoleMapper.toResponseDTOList(
                userRoleRepository.findAllByUserId(userId)
        );
    }

    @Transactional(readOnly = true)
    public List<RoleUserResponseDTO> findByRoleId(Long roleId) {
        return userRoleMapper.toRoleUserResponseDTOList(
                userRoleRepository.findAllByRoleId(roleId)
        );
    }

    @Transactional
    public void delete(Long userId, Long roleId) {
        UserRole userRole = findAssignment(userId, roleId);

        userRoleRepository.delete(userRole);
    }

    private UserRole findAssignment(Long userId, Long roleId) {
        return userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Asignación de rol no encontrada"
                        )
                );
    }
}
