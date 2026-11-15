package com.srh.cinepass.repository;

import com.srh.cinepass.entity.AutomationRun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutomationRunRepository extends JpaRepository<AutomationRun, Long> {

    /**
     * Most recent runs first, so the admin API can show the latest outcome
     * without loading the whole audit table.
     */
    List<AutomationRun> findTop50ByOrderByStartedAtDesc();
}
