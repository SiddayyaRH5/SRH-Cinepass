package com.srh.cinepass.support;

import com.srh.cinepass.util.BusinessTime;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/**
 * Replaces the production IST clock with a movable clock for tests. The bean is
 * primary so every service that injects {@code Clock} receives the test clock.
 */
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(Instant.now(), BusinessTime.ZONE);
    }
}
