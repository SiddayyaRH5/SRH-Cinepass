
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

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
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

                                                // Infrastructure and unauthenticated entry points.
                                                .requestMatchers(
                                                                "/error",
                                                                "/health",
                                                                "/api/health/live",
                                                                "/api/health/ready",
                                                                "/api/auth/login",
                                                                "/api/auth/signup")
                                                .permitAll()

                                                /*
                                                 * Razorpay calls this endpoint server-to-server. It is
                                                 * protected by HMAC signature verification inside the
                                                 * controller, not by a customer JWT.
                                                 */
                                                .requestMatchers("/api/payments/razorpay/webhook")
                                                .permitAll()

                                                // Public catalogue browsing is read-only.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/movies/**",
                                                                "/api/shows/**",
                                                                "/api/locations/**",
                                                                "/api/tmdb/search")
                                                .permitAll()

                                                // Theatre owners need their own theatres; this must be
                                                // declared before the public GET theatre rule.
                                                .requestMatchers(HttpMethod.GET, "/api/theatres/my")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/theatres/**")
                                                .permitAll()

                                                // Authenticated customer / theatre-owner endpoints.
                                                .requestMatchers("/api/auth/me").authenticated()
                                                .requestMatchers(
                                                                "/api/theatre-verification",
                                                                "/api/theatre-verification/",
                                                                "/api/theatre-verification/my",
                                                                "/api/theatre-verification/my/")
                                                .authenticated()

                                                // Only administrators may review applications.
                                                .requestMatchers("/api/theatre-verification/**")
                                                .hasRole("ADMIN")

                                                // Admin endpoints require an authenticated ADMIN user.
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                                // Only theatre staff and administrators may consume tickets.
                                                .requestMatchers("/api/staff/**")
                                                .hasAnyRole("STAFF", "ADMIN")

                                                // All booking and payment endpoints require JWT authentication.
                                                .requestMatchers("/api/bookings/**").authenticated()

                                                // Digital tickets are private to the authenticated customer.
                                                .requestMatchers("/api/tickets/**").authenticated()

                                                // Legacy TMDB sync uses its dedicated X-SYNC-KEY header.
                                                .requestMatchers(HttpMethod.POST, "/api/tmdb/sync/**")
                                                .permitAll()

                                                // Catalogue and location mutations are administrator-only.
                                                .requestMatchers("/api/movies/**", "/api/locations/**")
                                                .hasRole("ADMIN")

                                                /*
                                                 * Show and theatre mutations remain available to approved
                                                 * theatre owners and administrators; the service layer
                                                 * enforces ownership.
                                                 */
                                                .requestMatchers("/api/shows/**", "/api/theatres/**")
                                                .authenticated()

                                                // Fail closed for anything not explicitly listed above.
                                                .anyRequest().denyAll())

                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, exception) ->
                                                                writeJsonError(
                                                                                response,
                                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "Authentication required"))
                                                .accessDeniedHandler((request, response, exception) ->
                                                                writeJsonError(
                                                                                response,
                                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                                "You do not have permission to perform this action")))

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        private static void writeJsonError(
                        HttpServletResponse response,
                        int status,
                        String message) throws IOException {

                response.setStatus(status);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-store");
                response.getWriter().write("{\"message\":\"" + message + "\"}");
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
