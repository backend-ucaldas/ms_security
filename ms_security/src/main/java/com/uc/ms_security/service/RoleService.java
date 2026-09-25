package com.uc.ms_security.service;

import com.uc.ms_security.dto.RolePermissionsResponseDTO;
import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.exception.ApplicationException;
import com.uc.ms_security.exception.ErrorCase;
import com.uc.ms_security.mapper.RoleMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;

    public RoleResponseDTO create(RoleRequestDTO dto) {
        if (roleRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un rol con ese nombre"
            );
        }

        Role role = roleMapper.toEntity(dto);

        return roleMapper.toResponseDTO(
                roleRepository.save(role)
        );
    }

    public List<RoleResponseDTO> findAll() {
        return roleMapper.toResponseDTOList(
                roleRepository.findAll()
        );
    }

    public RoleResponseDTO findById(Long id) {
        return roleMapper.toResponseDTO(
                findEntityById(id)
        );
    }

    public RoleResponseDTO update(
            Long id,
            RoleRequestDTO dto) {

        Role role = findEntityById(id);

        if (roleRepository.existsByNameIgnoreCaseAndIdNot(
                dto.getName(), id)) {
            throw new ApplicationException(
                    ErrorCase.ALREADY_EXISTS,
                    "Ya existe un rol con ese nombre"
            );
        }

        roleMapper.updateEntity(dto, role);

        return roleMapper.toResponseDTO(
                roleRepository.save(role)
        );
    }

    public void delete(Long id) {
        Role role = findEntityById(id);

        if (userRoleRepository.existsByRoleId(id)) {
            throw new ApplicationException(
                    ErrorCase.INVALID_OPERATION,
                    "No se puede eliminar un rol que está asignado"
            );
        }

        roleRepository.delete(role);
    }

    private Role findEntityById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(
                        () -> new ApplicationException(
                                ErrorCase.NOT_FOUND,
                                "Rol no encontrado con id: " + id
                        )
                );
    }

    public RolePermissionsResponseDTO findByIdAndPermissions(Long id) {
        Role role = roleRepository
                .findWithPermissionsById(id)
                .orElseThrow(
                        () -> new ApplicationException(
                                ErrorCase.NOT_FOUND,
                                "Rol no encontrado con id: " + id
                        )
                );

        return roleMapper.toPermissionsResponseDTO(role);
    }
}
