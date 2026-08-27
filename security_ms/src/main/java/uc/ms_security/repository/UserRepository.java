package uc.security_ms.repository;

import uc.security_ms.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = {"profile"})
    Optional<User> findWithProfileById(Long id);
}
