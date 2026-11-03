package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}