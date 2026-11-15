package com.srh.cinepass.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} jobs. The jobs themselves are property-guarded so
 * tests and single-purpose deployments can disable them without changing code.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
