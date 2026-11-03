package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    boolean existsByNameIgnoreCaseAndLocationId(
            String name,
            Long locationId
    );

    boolean existsByNameIgnoreCaseAndLocationIdAndIdNot(
            String name,
            Long locationId,
            Long id
    );

    List<Theatre> findByLocationId(Long locationId);

    List<Theatre> findByOwnerId(Long ownerId);
}