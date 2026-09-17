package com.srh.cinepass.security;

import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.UserRepository;
import com.srh.cinepass.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class GoogleOAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public GoogleOAuth2SuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        // Get Google email
        String googleEmail =
                oauthUser.getAttribute("email");

        // Get Google name
        String googleName =
                oauthUser.getAttribute("name");

        // Validate email
        if (googleEmail == null ||
                googleEmail.isBlank()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Google account email was not provided"
            );

            return;
        }

        String email =
                googleEmail.trim().toLowerCase();

        // Find existing user
        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        // ============================
        // CREATE GOOGLE USER
        // ============================

        if (user == null) {

            user = new User();

            if (googleName != null &&
                    !googleName.isBlank()) {

                user.setName(
                        googleName.trim()
                );

            } else {

                user.setName("Google User");
            }

            user.setEmail(email);

            /*
             * Google users don't need a normal password.
             * A random BCrypt password is stored so the
             * database password column remains valid.
             */

            user.setPassword(
                    passwordEncoder.encode(
                            UUID.randomUUID().toString()
                    )
            );

            user.setRole("USER");

            user =
                    userRepository.save(user);
        }

        // ============================
        // GENERATE CINEPASS JWT
        // ============================

        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        // ============================
        // REDIRECT TO REACT
        // ============================

        String frontendUrl =
                "http://localhost:5173";

        String redirectUrl =
                frontendUrl +
                "/oauth2/success?token=" +
                token;

        response.sendRedirect(redirectUrl);
    }
}