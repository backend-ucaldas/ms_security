package com.uc.ms_security.controller;

import com.uc.ms_security.dto.ProfileRequestDTO;
import com.uc.ms_security.dto.ProfileResponseDTO;
import com.uc.ms_security.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{userId}/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponseDTO create(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileRequestDTO dto) {
        return profileService.create(userId, dto);
    }
       

    @PutMapping
    public ProfileResponseDTO update(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileRequestDTO dto) {
        return profileService.update(userId, dto);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        profileService.delete(userId);
    }
}
