package com.uc.ms_security.controller;

import com.uc.ms_security.dto.AssignPermissionRequestDTO;
import com.uc.ms_security.dto.PermissionRoleResponseDTO;
import com.uc.ms_security.dto.RolePermissionResponseDTO;
import com.uc.ms_security.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponseDTO assign(
            @Valid @RequestBody AssignPermissionRequestDTO dto) {

        return rolePermissionService.assign(dto);
    }

    @GetMapping
    public List<RolePermissionResponseDTO> findAll() {
        return rolePermissionService.findAll();
    }

    @GetMapping("/roles/{roleId}")
    public List<RolePermissionResponseDTO> findByRoleId(
            @PathVariable Long roleId) {

        return rolePermissionService.findByRoleId(roleId);
    }

    @GetMapping("/permissions/{permissionId}")
    public List<PermissionRoleResponseDTO> findByPermissionId(
            @PathVariable Long permissionId) {

        return rolePermissionService.findByPermissionId(permissionId);
    }

    @DeleteMapping("/{roleId}/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {

        rolePermissionService.delete(roleId, permissionId);
    }
}
