package com.srh.cinepass.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("movies", "locations");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(Duration.ofMinutes(3))
                .recordStats());
        return manager;
    }
}
