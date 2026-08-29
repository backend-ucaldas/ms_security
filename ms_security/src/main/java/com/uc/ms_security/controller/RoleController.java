package com.uc.ms_security.controller;

import com.uc.ms_security.dto.RoleRequestDTO;
import com.uc.ms_security.dto.RoleResponseDTO;
import com.uc.ms_security.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponseDTO create(
            @Valid @RequestBody RoleRequestDTO dto) {
        return roleService.create(dto);
    }

    @GetMapping
    public List<RoleResponseDTO> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    public RoleResponseDTO findById(
            @PathVariable Long id) {
        return roleService.findById(id);
    }

    @PutMapping("/{id}")
    public RoleResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequestDTO dto) {
        return roleService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
