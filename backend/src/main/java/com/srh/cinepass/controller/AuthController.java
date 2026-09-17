package com.srh.cinepass.controller;

import com.srh.cinepass.dto.LoginRequest;
import com.srh.cinepass.dto.LoginResponse;
import com.srh.cinepass.dto.SignupRequest;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ============================
    // NORMAL SIGNUP
    // ============================

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody SignupRequest request) {

        try {

            User user = authService.signup(request);

            // Do NOT return password/hash to frontend
            return ResponseEntity.ok(
                    new LoginResponse(
                            null,
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getRole()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================
    // NORMAL LOGIN
    // ============================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            LoginResponse response =
                    authService.login(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ============================
    // CURRENT LOGGED-IN USER
    // ============================

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return ResponseEntity
                    .status(401)
                    .body("User is not authenticated");
        }

        User user =
                (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                new LoginResponse(
                        null,
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                )
        );
    }
}