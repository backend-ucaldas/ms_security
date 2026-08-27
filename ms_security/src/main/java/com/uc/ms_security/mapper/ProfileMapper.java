package com.uc.ms_security.mapper;

import com.uc.ms_security.dto.ProfileRequestDTO;
import com.uc.ms_security.dto.ProfileResponseDTO;
import com.uc.ms_security.entity.Profile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public Profile toEntity(
            ProfileRequestDTO dto) {

        Profile profile =
                new Profile();

        profile.setPhone(
                dto.getPhone()
        );

        profile.setBirthDate(
                dto.getBirthDate()
        );

        return profile;
    }

    public void updateEntity(
            ProfileRequestDTO dto,
            Profile profile) {

        profile.setPhone(
                dto.getPhone()
        );

        profile.setBirthDate(
                dto.getBirthDate()
        );
    }

    public ProfileResponseDTO toResponseDTO(
            Profile profile) {

        if (profile == null) {
            return null;
        }

        return new ProfileResponseDTO(
                profile.getId(),
                profile.getPhone(),
                profile.getBirthDate()
        );
    }
}