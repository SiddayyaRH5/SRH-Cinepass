
package com.srh.cinepass.config;

import com.srh.cinepass.filter.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

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

                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers(
                                                                "/health",
                                                                "/api/auth/login",
                                                                "/api/auth/signup",
                                                                "/api/auth/me")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/api/movies/**",
                                                                "/api/theatres/**",
                                                                "/api/shows/**",
                                                                "/api/locations/**")
                                                .permitAll()

                                                // Admin endpoints require an authenticated ADMIN user.
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                                // All booking and payment endpoints require JWT authentication.
                                                .requestMatchers("/api/bookings/**").authenticated()

                                                .anyRequest().permitAll())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

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

                configuration.setAllowedOrigins(
                                origins.stream().distinct().toList());

                configuration.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PUT",
                                "DELETE",
                                "PATCH",
                                "OPTIONS"));

                configuration.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "Origin"));

                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
