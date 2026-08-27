package uc.security_ms.repository;

import uc.security_ms.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
