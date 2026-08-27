package uc.security_ms.controller;

import uc.ms_security.dto.;
import uc.security_ms.dto.ProfileResponseDTO;
import uc.security_ms.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping
    public ProfileResponseDTO find(@PathVariable Long userId) {
        return profileService.findByUserId(userId);
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
