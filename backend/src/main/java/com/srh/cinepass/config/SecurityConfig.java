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
            // =====================================================
            // CSRF
            // =====================================================
            .csrf(csrf -> csrf.disable())

            // =====================================================
            // CORS
            // =====================================================
            .cors(cors ->
                cors.configurationSource(corsConfigurationSource())
            )

            // =====================================================
            // SESSION
            // =====================================================
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            // =====================================================
            // AUTHORIZATION
            // =====================================================
            .authorizeHttpRequests(auth -> {

                // =================================================
                // AUTHENTICATION
                // =================================================

                auth.requestMatchers(
                    "/api/auth/login",
                    "/api/auth/signup"
                ).permitAll();

                auth.requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll();

                auth.requestMatchers(
                    "/api/auth/me"
                ).authenticated();


                // =================================================
                // MOVIES
                // =================================================

                // Anyone can view movies
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/movies/**"
                ).permitAll();

                // Admin can create movies
                auth.requestMatchers(
                    HttpMethod.POST,
                    "/api/movies/**"
                ).hasRole("ADMIN");

                // Admin can update movies
                auth.requestMatchers(
                    HttpMethod.PUT,
                    "/api/movies/**"
                ).hasRole("ADMIN");

                // Admin can delete movies
                auth.requestMatchers(
                    HttpMethod.DELETE,
                    "/api/movies/**"
                ).hasRole("ADMIN");


                // =================================================
                // LOCATIONS
                // =================================================

                // Anyone can view locations
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/locations/**"
                ).permitAll();

                // Admin can create locations
                auth.requestMatchers(
                    HttpMethod.POST,
                    "/api/locations/**"
                ).hasRole("ADMIN");

                // Admin can update locations
                auth.requestMatchers(
                    HttpMethod.PUT,
                    "/api/locations/**"
                ).hasRole("ADMIN");

                // Admin can delete locations
                auth.requestMatchers(
                    HttpMethod.DELETE,
                    "/api/locations/**"
                ).hasRole("ADMIN");


                // =================================================
                // THEATRES
                // =================================================

                // Theatre owner can view own theatres
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/theatres/my"
                ).hasRole("THEATRE_OWNER");

                // Anyone can view theatres
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/theatres/**"
                ).permitAll();

                // Admin or theatre owner can create theatres
                auth.requestMatchers(
                    HttpMethod.POST,
                    "/api/theatres/**"
                ).hasAnyRole(
                    "ADMIN",
                    "THEATRE_OWNER"
                );

                // Admin or theatre owner can update theatres
                auth.requestMatchers(
                    HttpMethod.PUT,
                    "/api/theatres/**"
                ).hasAnyRole(
                    "ADMIN",
                    "THEATRE_OWNER"
                );

                // Admin or theatre owner can delete theatres
                auth.requestMatchers(
                    HttpMethod.DELETE,
                    "/api/theatres/**"
                ).hasAnyRole(
                    "ADMIN",
                    "THEATRE_OWNER"
                );


                // =================================================
                // SHOWS
                // =================================================

                // Anyone can view shows and seats
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/shows/**"
                ).permitAll();

                // Admin can create shows
                auth.requestMatchers(
                    HttpMethod.POST,
                    "/api/shows/**"
                ).hasRole("ADMIN");

                // Admin can update shows
                auth.requestMatchers(
                    HttpMethod.PUT,
                    "/api/shows/**"
                ).hasRole("ADMIN");

                // Admin can delete shows
                auth.requestMatchers(
                    HttpMethod.DELETE,
                    "/api/shows/**"
                ).hasRole("ADMIN");


                // =================================================
                // BOOKINGS
                // =================================================

                // Logged-in users can see their bookings
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/bookings/me"
                ).authenticated();

                // Only admin can see all bookings
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/bookings"
                ).hasRole("ADMIN");

                // Logged-in users can manage bookings
                auth.requestMatchers(
                    "/api/bookings/**"
                ).authenticated();


                // =================================================
                // THEATRE VERIFICATION
                // =================================================

                // Theatre owner submits verification
                auth.requestMatchers(
                    HttpMethod.POST,
                    "/api/theatre-verification"
                ).hasRole("THEATRE_OWNER");

                // Theatre owner sees own verification
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/theatre-verification/my"
                ).hasRole("THEATRE_OWNER");

                // Admin sees pending verifications
                auth.requestMatchers(
                    HttpMethod.GET,
                    "/api/theatre-verification/pending"
                ).hasRole("ADMIN");

                // Admin approves verification
                auth.requestMatchers(
                    HttpMethod.PUT,
                    "/api/theatre-verification/*/approve"
                ).hasRole("ADMIN");

                // Admin rejects verification
                auth.requestMatchers(
                    HttpMethod.PUT,
                    "/api/theatre-verification/*/reject"
                ).hasRole("ADMIN");


                // =================================================
                // CORS PREFLIGHT
                // =================================================

                auth.requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                ).permitAll();


                // =================================================
                // EVERYTHING ELSE
                // =================================================

                auth.anyRequest().authenticated();
            })

            // =====================================================
            // GOOGLE OAUTH2
            // =====================================================
            .oauth2Login(oauth2 ->
                oauth2.successHandler(
                    googleAuthenticationSuccessHandler()
                )
            )

            // =====================================================
            // JWT FILTER
            // =====================================================
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }


    // =============================================================
    // GOOGLE OAUTH2 SUCCESS HANDLER
    // =============================================================

    private GoogleOAuth2SuccessHandler
    googleAuthenticationSuccessHandler() {

        return googleOAuth2SuccessHandler;
    }


    // =============================================================
    // CORS CONFIGURATION
    // =============================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
            new CorsConfiguration();

        // ---------------------------------------------------------
        // FRONTEND URLs
        // ---------------------------------------------------------

        configuration.setAllowedOrigins(
            List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://srh-cinepass.vercel.app"
            )
        );


        // ---------------------------------------------------------
        // ALLOWED METHODS
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // ALLOWED HEADERS
        // ---------------------------------------------------------

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin"
            )
        );


        // ---------------------------------------------------------
        // EXPOSED HEADERS
        // ---------------------------------------------------------

        configuration.setExposedHeaders(
            List.of("Authorization")
        );


        // ---------------------------------------------------------
        // ALLOW CREDENTIALS
        // ---------------------------------------------------------

        configuration.setAllowCredentials(true);


        // ---------------------------------------------------------
        // REGISTER CORS
        // ---------------------------------------------------------

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }
}