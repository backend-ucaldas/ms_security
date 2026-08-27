package uc.security_ms.mapper;

import uc.security_ms.dto.ProfileRequestDTO;
import uc.security_ms.dto.ProfileResponseDTO;
import uc.security_ms.entity.Profile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public Profile toEntity(ProfileRequestDTO dto) {
        Profile profile = new Profile();
        profile.setPhone(dto.getPhone());
        profile.setBirthDate(dto.getBirthDate());
        return profile;
    }

    public void updateEntity(ProfileRequestDTO dto, Profile profile) {
        profile.setPhone(dto.getPhone());
        profile.setBirthDate(dto.getBirthDate());
    }

    public ProfileResponseDTO toResponseDTO(Profile profile) {
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
