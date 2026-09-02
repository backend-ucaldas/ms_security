package com.uc.ms_security.service;

import com.uc.ms_security.dto.PermissionRequestDTO;
import com.uc.ms_security.dto.PermissionResponseDTO;
import com.uc.ms_security.entity.Permission;
import com.uc.ms_security.mapper.PermissionMapper;
import com.uc.ms_security.repository.PermissionRepository;
import com.uc.ms_security.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionMapper permissionMapper;

    public boolean hasPermission(
            Long userId,
            String httpMethod,
            String requestUrl) {

        String normalizedUrl = normalizeUrl(requestUrl);

        return permissionRepository
                .findByUserIdAndMethodAndUrl(
                        userId,
                        httpMethod.toUpperCase(Locale.ROOT),
                        normalizedUrl
                )
                .stream()
                .findAny()
                .isPresent();
    }

    public PermissionResponseDTO create(PermissionRequestDTO dto) {
        if (permissionRepository.existsByUrlAndMethod(
                dto.getUrl(), dto.getMethod())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un permiso con esa url y método"
            );
        }

        Permission permission = permissionMapper.toEntity(dto);

        return permissionMapper.toResponseDTO(
                permissionRepository.save(permission)
        );
    }

    public List<PermissionResponseDTO> findAll() {
        return permissionMapper.toResponseDTOList(
                permissionRepository.findAll()
        );
    }

    public PermissionResponseDTO findById(Long id) {
        return permissionMapper.toResponseDTO(
                findEntityById(id)
        );
    }

    public PermissionResponseDTO update(
            Long id,
            PermissionRequestDTO dto) {

        Permission permission = findEntityById(id);

        if (permissionRepository.existsByUrlAndMethodAndIdNot(
                dto.getUrl(), dto.getMethod(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un permiso con esa url y método"
            );
        }

        permissionMapper.updateEntity(dto, permission);

        return permissionMapper.toResponseDTO(
                permissionRepository.save(permission)
        );
    }

    public void delete(Long id) {
        Permission permission = findEntityById(id);

        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar un permiso que está asignado"
            );
        }

        permissionRepository.delete(permission);
    }

    private Permission findEntityById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Permiso no encontrado"
                        )
                );
    }

        private String normalizeUrl(String requestUrl) {
                return requestUrl.replaceAll(
                                "(?i)(?<=/)(?:\\d+|[a-f0-9]{24})(?=/|$)",
                                "{id}"
                );
        }
}
