package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    List<Theatre> findByLocationId(Long locationId);
}