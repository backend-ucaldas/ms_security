package uc.security_ms.service;

import uc.security_ms.dto.ProfileRequestDTO;
import uc.security_ms.dto.ProfileResponseDTO;
import uc.security_ms.entity.Profile;
import uc.security_ms.entity.User;
import uc.security_ms.mapper.ProfileMapper;
import uc.security_ms.repository.ProfileRepository;
import uc.security_ms.repository.UserRepository;
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

        public ProfileResponseDTO findByUserId(Long userId) {
            return profileMapper.toResponseDTO(findProfile(userId));
        }

        public ProfileResponseDTO update(Long userId, ProfileRequestDTO dto) {
            Profile profile = findProfile(userId);
            profileMapper.updateEntity(dto, profile);
            Profile updatedProfile = profileRepository.save(profile);
            return profileMapper.toResponseDTO(updatedProfile);
        }

        public void delete(Long userId) {
            Profile profile = findProfile(userId);
            profileRepository.delete(profile);
        }

        private Profile findProfile(Long userId) {
            return profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Perfil no encontrado"
                    ));
        }
    }
