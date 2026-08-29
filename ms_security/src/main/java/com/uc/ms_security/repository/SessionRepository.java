package com.uc.ms_security.repository;

import com.uc.ms_security.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository
        extends JpaRepository<Session, Long> {

    List<Session> findAllByUserId(Long userId);

    Optional<Session> findByIdAndUserId(
            Long sessionId,
            Long userId
    );

    boolean existsByToken(String token);

    boolean existsByTokenAndIdNot(
            String token,
            Long id
    );
}