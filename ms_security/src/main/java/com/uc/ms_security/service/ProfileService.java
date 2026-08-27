package com.uc.ms_security.service;

import com.uc.ms_security.dto.ProfileRequestDTO;
import com.uc.ms_security.dto.ProfileResponseDTO;
import com.uc.ms_security.entity.Profile;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.mapper.ProfileMapper;
import com.uc.ms_security.repository.ProfileRepository;
import com.uc.ms_security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    public ProfileResponseDTO create(Long userId, ProfileRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        if (profileRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario ya tiene un perfil"
            );
        }

        Profile profile = profileMapper.toEntity(dto);
        profile.setUser(user);

        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponseDTO(savedProfile);
    }

    public ProfileResponseDTO update(Long userId, ProfileRequestDTO dto) {
        Profile profile = findProfile(userId);

        profileMapper.updateEntity(dto, profile);
        Profile updatedProfile = profileRepository.save(profile);

        return profileMapper.toResponseDTO(updatedProfile);
    }

    public ProfileResponseDTO findByUserId(Long userId) {
        return profileMapper.toResponseDTO(findProfile(userId));
    }

    public void delete(Long userId) {
        profileRepository.delete(findProfile(userId));
    }

    private Profile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Perfil no encontrado"
                ));
    }
}