package com.srh.cinepass.service;

import com.srh.cinepass.dto.LoginRequest;
import com.srh.cinepass.dto.LoginResponse;
import com.srh.cinepass.dto.SignupRequest;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User signup(SignupRequest request) {

        if (request.getName() == null ||
            request.getName().isBlank()) {
            throw new RuntimeException("Name is required");
        }

        if (request.getEmail() == null ||
            request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (request.getPassword() == null ||
            request.getPassword().length() < 6) {
            throw new RuntimeException(
                "Password must contain at least 6 characters"
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException(
                "An account with this email already exists"
            );
        }

        /*
         * Public signup can create only USER
         * or THEATRE_OWNER.
         * ADMIN cannot be created from signup.
         */
        String role =
            "THEATRE_OWNER".equalsIgnoreCase(request.getRole())
                ? "THEATRE_OWNER"
                : "USER";

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);

        user.setPassword(
            passwordEncoder.encode(
                request.getPassword()
            )
        );

        user.setRole(role);

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {

        if (request.getEmail() == null ||
            request.getPassword() == null) {
            throw new RuntimeException(
                "Email and password are required"
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                    new RuntimeException(
                        "Invalid email or password"
                    )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                "Invalid email or password"
            );
        }

        String token =
            jwtService.generateToken(
                user.getEmail()
            );

        return new LoginResponse(
            token,
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole()
        );
    }
}