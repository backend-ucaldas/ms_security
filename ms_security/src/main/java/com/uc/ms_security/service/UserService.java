package com.uc.ms_security.service;

import com.uc.ms_security.dto.*;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public UserResponseDTO create(CreateUserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con este email"
            );
        }
        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        return userMapper.toResponseDTO(savedUser);
    }

    public List<UserResponseDTO> findAll() {
        List<User> users =userRepository.findAll();
        return userMapper.toResponseDTOList(users);
    }
    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));
    }

    public UserResponseDTO findById(Long id) {
        User user = findUser(id);
        return userMapper.toResponseDTO(user);
    }

    public UserDetailResponseDTO findByIdAndProfile(Long id) {
        User user =userRepository
                        .findWithProfileById(id)
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Usuario no encontrado"
                                )
                        );

        return userMapper.toDetailResponseDTO(user);
    }

    public UserResponseDTO update(Long id, UpdateUserDTO dto) {
        User user = findUser(id);
        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El email pertenece a otro usuario"
            );
        }
        userMapper.updateEntity(dto, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }
    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }
    public UserSessionsResponseDTO findByIdAndSessions(Long id) {
        User user = userRepository
                .findWithSessionsById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Usuario no encontrado"
                        )
                );

        return userMapper.toSessionsResponseDTO(user);
    }
}