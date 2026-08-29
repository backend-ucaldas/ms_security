package com.uc.ms_security.service;

import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.entity.Role;
import com.uc.ms_security.mapper.RoleMapper;
import com.uc.ms_security.repository.RoleRepository;
import com.uc.ms_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;

    public RoleResponseDTO create(RoleRequestDTO dto) {
        if (roleRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar un rol que está asignado"
            );
        }

        roleRepository.delete(role);
    }

    private Role findEntityById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Rol no encontrado"
                        )
                );
    }
}
