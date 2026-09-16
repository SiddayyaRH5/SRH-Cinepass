package com.srh.cinepass.service;

import com.srh.cinepass.dto.LoginRequest;
import com.srh.cinepass.dto.LoginResponse;
import com.srh.cinepass.dto.SignupRequest;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // =========================
    // SIGNUP
    // =========================

    public User signup(SignupRequest request) {

        if (request.getName() == null ||
                request.getName().isBlank()) {

            throw new RuntimeException(
                    "Name is required");
        }

        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new RuntimeException(
                    "Email is required");
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {

            throw new RuntimeException(
                    "Password is required");
        }

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {

            throw new RuntimeException(
                    "Email is already registered");
        }

        User user = new User();

        user.setName(request.getName().trim());

        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()));

        user.setRole("USER");

        return userRepository.save(user);
    }

    // =========================
    // LOGIN
    // =========================

    public LoginResponse login(LoginRequest request) {

        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new RuntimeException(
                    "Email is required");
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {

            throw new RuntimeException(
                    "Password is required");
        }

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "Invalid email or password"));

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword());

        if (!passwordMatches) {

            throw new RuntimeException(
                    "Invalid email or password");
        }

        // Generate JWT token
        String token = jwtService.generateToken(
                user.getEmail());

        // Return token + safe user information
        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }
}