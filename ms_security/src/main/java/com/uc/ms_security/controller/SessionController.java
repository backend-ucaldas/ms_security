package com.uc.ms_security.controller;

import com.uc.ms_security.dto.SessionRequestDTO;
import com.uc.ms_security.dto.SessionResponseDTO;
import com.uc.ms_security.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponseDTO create(
            @PathVariable Long userId,
            @Valid @RequestBody SessionRequestDTO dto) {

        return sessionService.create(userId, dto);
    }

    @GetMapping
    public List<SessionResponseDTO> findAll(
            @PathVariable Long userId) {

        return sessionService.findAllByUserId(userId);
    }

    @GetMapping("/{sessionId}")
    public SessionResponseDTO findById(
            @PathVariable Long userId,
            @PathVariable Long sessionId) {

        return sessionService.findById(userId, sessionId);
    }

    @PutMapping("/{sessionId}")
    public SessionResponseDTO update(
            @PathVariable Long userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequestDTO dto) {

        return sessionService.update(
                userId,
                sessionId,
                dto
        );
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long userId,
            @PathVariable Long sessionId) {

        sessionService.delete(userId, sessionId);
    }
}