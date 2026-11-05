package com.srh.cinepass.config;

import com.srh.cinepass.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String frontendUrls;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrls) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.frontendUrls = frontendUrls;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/api/health/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                // User-specific routes must precede the public catalogue matchers.
                .requestMatchers(HttpMethod.GET, "/api/theatres/my").authenticated()

                // Public catalogue reads.
                .requestMatchers(HttpMethod.GET, "/api/movies", "/api/movies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/locations", "/api/locations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/shows", "/api/shows/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/theatres", "/api/theatres/location/**", "/api/theatres/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tmdb/search").permitAll()

                // User-specific and administrative resources.
                .requestMatchers(HttpMethod.GET, "/api/theatre-verification/pending").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/theatre-verification/**").hasRole("ADMIN")
                .requestMatchers("/api/theatre-verification/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/bookings/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/bookings").authenticated()
                .requestMatchers("/api/bookings", "/api/bookings/**").hasRole("ADMIN")

                // Catalogue writes are administrator-only.
                .requestMatchers("/api/movies/**", "/api/locations/**").hasRole("ADMIN")
                .requestMatchers("/api/theatres/**", "/api/shows/**").authenticated()
                // TMDB sync has its own constant-time shared-key check.
                .requestMatchers("/api/tmdb/sync/**").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://srh-cinepass.vercel.app"));
        if (frontendUrls != null && !frontendUrls.isBlank()) {
            Arrays.stream(frontendUrls.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(origins::add);
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins.stream().distinct().toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-SYNC-KEY"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
