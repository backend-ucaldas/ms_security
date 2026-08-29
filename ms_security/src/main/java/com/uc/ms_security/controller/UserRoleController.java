package com.uc.ms_security.controller;

import com.uc.ms_security.dto.AssignRoleRequestDTO;
import com.uc.ms_security.dto.RoleUserResponseDTO;
import com.uc.ms_security.dto.UserRoleResponseDTO;
import com.uc.ms_security.service.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserRoleResponseDTO assign(
            @Valid @RequestBody AssignRoleRequestDTO dto) {

        return userRoleService.assign(dto);
    }

    @GetMapping
    public List<UserRoleResponseDTO> findAll() {
        return userRoleService.findAll();
    }

    @GetMapping("/users/{userId}")
    public List<UserRoleResponseDTO> findByUserId(
            @PathVariable Long userId) {

        return userRoleService.findByUserId(userId);
    }

    @GetMapping("/roles/{roleId}")
    public List<RoleUserResponseDTO> findByRoleId(
            @PathVariable Long roleId) {

        return userRoleService.findByRoleId(roleId);
    }

    @DeleteMapping("/{userId}/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        userRoleService.delete(userId, roleId);
    }
}
