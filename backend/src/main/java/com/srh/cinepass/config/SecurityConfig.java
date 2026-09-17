package com.srh.cinepass.config;

import com.srh.cinepass.filter.JwtAuthenticationFilter;
import com.srh.cinepass.security.GoogleOAuth2SuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

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
            .cors(cors ->
                cors.configurationSource(
                    corsConfigurationSource()
                )
            )

            // JWT + OAuth2 need session support
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            .authorizeHttpRequests(auth -> auth

                // =========================================
                // AUTHENTICATION
                // =========================================

                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/signup"
                ).permitAll()

                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()

                .requestMatchers(
                    "/api/auth/me"
                ).authenticated()


                // =========================================
                // MOVIES
                // =========================================

                // Anyone can view movies
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/movies/**"
                ).permitAll()

                // Only ADMIN can modify movies
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/movies/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/movies/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/movies/**"
                ).hasRole("ADMIN")


                // =========================================
                // LOCATIONS
                // =========================================

                // Anyone can view locations
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/locations/**"
                ).permitAll()

                // Only ADMIN can modify locations
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/locations/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/locations/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/locations/**"
                ).hasRole("ADMIN")


                // =========================================
                // THEATRES
                // =========================================

                // Anyone can view theatres
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/theatres/my"
                ).hasRole("THEATRE_OWNER")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/theatres/**"
                ).permitAll()

                // Only ADMIN can add theatres
                .requestMatchers(HttpMethod.POST, "/api/theatres/**").hasAnyRole("ADMIN", "THEATRE_OWNER")

                // Only ADMIN can edit theatres
                .requestMatchers(HttpMethod.PUT, "/api/theatres/**").hasAnyRole("ADMIN", "THEATRE_OWNER")

                // Only ADMIN can delete theatres
                .requestMatchers(HttpMethod.DELETE, "/api/theatres/**").hasAnyRole("ADMIN", "THEATRE_OWNER")


                // =========================================
                // SHOWS
                // =========================================

                // Anyone can view shows and seats
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/shows/**"
                ).permitAll()

                // Only ADMIN can create shows
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/shows/**"
                ).hasRole("ADMIN")

                // Only ADMIN can edit shows
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/shows/**"
                ).hasRole("ADMIN")

                // Only ADMIN can delete shows
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/shows/**"
                ).hasRole("ADMIN")


                // =========================================
                // BOOKINGS
                // =========================================

                // Normal users can see their own bookings
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/bookings/me"
                ).authenticated()

                // Only ADMIN can see every customer's booking
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/bookings"
                ).hasRole("ADMIN")

                // Logged-in users can create/manage booking APIs
                .requestMatchers(
                    "/api/bookings/**"
                ).authenticated()


                // =========================================
                // OPTIONS / CORS
                // =========================================

                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                ).permitAll()


                // =========================================
                // EVERYTHING ELSE
                // =========================================

                .anyRequest().authenticated()
            )

            // Google Login
            .oauth2Login(oauth2 ->
                oauth2.successHandler(
                    googleAuthenticationSuccessHandler()
                )
            )

            // JWT filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }


    /*
     * Google OAuth2 success handler
     *
     * The existing bean is injected above.
     */
    private GoogleOAuth2SuccessHandler
    googleAuthenticationSuccessHandler() {

        return googleOAuth2SuccessHandler;
    }


    // =========================================
    // CORS
    // =========================================

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {

        CorsConfiguration configuration =
            new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
            )
        );

        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin"
            )
        );

        configuration.setExposedHeaders(
            List.of("Authorization")
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }
}
