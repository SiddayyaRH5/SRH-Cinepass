package com.srh.cinepass.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Centralized cache configuration.
 *
 * <p>Uses Caffeine as the cache provider. Each cache has its own TTL and
 * maximum size, tuned for the data it holds:</p>
 * <ul>
 *   <li><b>movies</b> — public movie catalogue (3 min TTL, 500 entries).</li>
 *   <li><b>movie-details</b> — individual movie details (5 min TTL, 1000 entries).</li>
 *   <li><b>theatres</b> — public theatre listings (5 min TTL, 200 entries).</li>
 *   <li><b>showtimes</b> — show listings by movie/date (60 s TTL, 500 entries).</li>
 * </ul>
 *
 * <p>Redis can be enabled by adding {@code spring-boot-starter-data-redis} to
 * the classpath and setting {@code spring.cache.type=redis} plus the Redis
 * connection properties. When Redis is not configured, Caffeine is used
 * automatically so local development works without any external service.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager(
                "movies", "movie-details", "theatres", "showtimes") {

            @Override
            protected CaffeineCache createCaffeineCache(String name) {
                Caffeine<Object, Object> builder = Caffeine.newBuilder()
                        .recordStats();

                switch (name) {
                    case "movies":
                        return new CaffeineCache(name, builder
                                .maximumSize(500)
                                .expireAfterWrite(Duration.ofMinutes(3))
                                .build());
                    case "movie-details":
                        return new CaffeineCache(name, builder
                                .maximumSize(1000)
                                .expireAfterWrite(Duration.ofMinutes(5))
                                .build());
                    case "theatres":
                        return new CaffeineCache(name, builder
                                .maximumSize(200)
                                .expireAfterWrite(Duration.ofMinutes(5))
                                .build());
                    case "showtimes":
                        return new CaffeineCache(name, builder
                                .maximumSize(500)
                                .expireAfterWrite(Duration.ofSeconds(60))
                                .build());
                    default:
                        return new CaffeineCache(name, builder
                                .maximumSize(300)
                                .expireAfterWrite(Duration.ofMinutes(3))
                                .build());
                }
            }
        };
    }
}
