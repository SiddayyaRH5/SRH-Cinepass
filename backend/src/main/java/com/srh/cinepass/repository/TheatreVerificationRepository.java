package com.srh.cinepass.repository;

import com.srh.cinepass.entity.TheatreVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TheatreVerificationRepository
        extends JpaRepository<TheatreVerification, Long> {

    Optional<TheatreVerification> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndStatus(Long userId, String status);

    List<TheatreVerification> findByStatusOrderBySubmittedAtAsc(String status);
}