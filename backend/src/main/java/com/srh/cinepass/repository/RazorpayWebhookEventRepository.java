package com.srh.cinepass.repository;

import com.srh.cinepass.entity.RazorpayWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RazorpayWebhookEventRepository
        extends JpaRepository<RazorpayWebhookEvent, Long> {

    Optional<RazorpayWebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
