package com.uc.ms_security.controller;

import com.uc.ms_security.dto.*;
import com.uc.ms_security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO create(@Valid @RequestBody CreateUserDTO dto) {
        return userService.create(dto);
    }

    @GetMapping
    public List<UserResponseDTO> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponseDTO findById(@PathVariable Long id) {
        return userService.findById(id);
    }
    @GetMapping("/{id}/profile")
    public UserDetailResponseDTO findByIdAndProfile(
            @PathVariable Long id) {

        return userService.findByIdAndProfile(id);
    }

    @PutMapping("/{id}")
    public UserResponseDTO update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO dto) {
        return userService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/{id}/detail-with-sessions")
    public UserSessionsResponseDTO findByIdAndSessions(@PathVariable Long id) {

        return userService.findByIdAndSessions(id);
    }

    @GetMapping("/{id}/detail-with-roles")
    public UserRolesResponseDTO findByIdAndRoles(
            @PathVariable Long id) {

        return userService.findByIdAndRoles(id);
    }
}
