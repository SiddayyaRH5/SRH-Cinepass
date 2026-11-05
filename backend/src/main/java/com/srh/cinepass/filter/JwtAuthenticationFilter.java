package com.srh.cinepass.filter;

import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.UserRepository;
import com.srh.cinepass.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if ("/health".equals(path)
                || path.startsWith("/api/health/")
                || "/api/auth/login".equals(path)
                || "/api/auth/signup".equals(path)) {
            return true;
        }

        // These GET endpoints are intentionally public. Do not perform an
        // unnecessary user-table lookup just because an old token is stored
        // in the visitor's browser.
        if (HttpMethod.GET.matches(request.getMethod())) {
            if (path.equals("/api/movies") || path.startsWith("/api/movies/")
                    || path.equals("/api/locations") || path.startsWith("/api/locations/")
                    || path.equals("/api/shows") || path.startsWith("/api/shows/")
                    || path.equals("/api/tmdb/search")
                    || path.equals("/api/theatres")
                    || (!path.equals("/api/theatres/my") && path.matches("/api/theatres/[^/]+"))
                    || path.matches("/api/theatres/location/[^/]+")
                    || path.matches("/api/theatres/[^/]+")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        // No Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Remove "Bearer "
            String token = authorizationHeader.substring(7);

            // Get email from JWT
            String email = jwtService.extractEmail(token);

            // Only authenticate if no authentication exists
            if (email != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                User user = userRepository.findByEmail(email)
                        .orElse(null);

                if (user != null &&
                        jwtService.isTokenValid(
                                token,
                                user.getEmail())) {

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" +
                                                    user.getRole())));

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication);
                }
            }

        } catch (Exception e) {

            // Invalid token.
            // Continue without authentication.
        }

        filterChain.doFilter(request, response);
    }
}