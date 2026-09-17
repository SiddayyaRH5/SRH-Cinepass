package com.srh.cinepass.config;

import com.srh.cinepass.filter.JwtAuthenticationFilter;
import com.srh.cinepass.security.GoogleOAuth2SuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleOAuth2SuccessHandler = googleOAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // Disable CSRF for REST API
                .csrf(csrf -> csrf.disable())

                // Enable CORS
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()))

                // Session support for OAuth2
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.IF_REQUIRED))

                // Authorization
                .authorizeHttpRequests(auth -> auth

                        // =========================================
                        // AUTHENTICATION
                        // =========================================
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/signup")
                        .permitAll()

                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**")
                        .permitAll()

                        .requestMatchers(
                                "/api/auth/me")
                        .authenticated()

                        // =========================================
                        // MOVIES
                        // =========================================

                        // Anyone can view movies
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/movies/**")
                        .permitAll()

                        // ADMIN can modify movies
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/movies/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/movies/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/movies/**")
                        .hasRole("ADMIN")

                        // =========================================
                        // LOCATIONS
                        // =========================================

                        // Anyone can view locations
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/locations/**")
                        .permitAll()

                        // ADMIN can modify locations
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/locations/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/locations/**")
                        .hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/locations/**")
                        .hasRole("ADMIN")

                        // =========================================
                        // THEATRES
                        // =========================================

                        // Theatre owner can see own theatres
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/theatres/my")
                        .hasRole("THEATRE_OWNER")

                        // Anyone can view theatres
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/theatres/**")
                        .permitAll()

                        // ADMIN or THEATRE_OWNER can create theatres
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/theatres/**")
                        .hasAnyRole("ADMIN", "THEATRE_OWNER")

                        // ADMIN or THEATRE_OWNER can update theatres
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/theatres/**")
                        .hasAnyRole("ADMIN", "THEATRE_OWNER")

                        // ADMIN or THEATRE_OWNER can delete theatres
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/theatres/**")
                        .hasAnyRole("ADMIN", "THEATRE_OWNER")

                        // =========================================
                        // SHOWS
                        // =========================================

                        // Anyone can view shows and seats
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/shows/**")
                        .permitAll()

                        // ADMIN can create shows
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/shows/**")
                        .hasRole("ADMIN")

                        // ADMIN can update shows
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/shows/**")
                        .hasRole("ADMIN")

                        // ADMIN can delete shows
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/shows/**")
                        .hasRole("ADMIN")

                        // =========================================
                        // BOOKINGS
                        // =========================================

                        // Logged-in users can see their bookings
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/bookings/me")
                        .authenticated()

                        // ADMIN can see all bookings
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/bookings")
                        .hasRole("ADMIN")

                        // Logged-in users can use booking APIs
                        .requestMatchers(
                                "/api/bookings/**")
                        .authenticated()

                        // =========================================
                        // CORS OPTIONS
                        // =========================================

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**")
                        .permitAll()

                        // =========================================
                        // EVERYTHING ELSE
                        // =========================================

                        .anyRequest().authenticated())

                // =========================================
                // GOOGLE OAUTH2
                // =========================================

                .oauth2Login(oauth2 -> oauth2.successHandler(
                        googleAuthenticationSuccessHandler()))

                // =========================================
                // JWT FILTER
                // =========================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =========================================
    // GOOGLE OAUTH2 SUCCESS HANDLER
    // =========================================

    private GoogleOAuth2SuccessHandler googleAuthenticationSuccessHandler() {

        return googleOAuth2SuccessHandler;
    }

    // =========================================
    // CORS CONFIGURATION
    // =========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://srh-cinepass.vercel.app"));

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"));

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin"));

        configuration.setExposedHeaders(
                List.of("Authorization"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration);

        return source;
    }
}