package com.srh.cinepass.config;

import com.srh.cinepass.util.BusinessTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The application always evaluates business timestamps in IST, even when the
 * JVM default timezone is UTC (as it is on Render). Injecting the clock also
 * lets tests fix "now" without touching the operating system.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock businessClock() {
        return BusinessTime.clock();
    }
}
