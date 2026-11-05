package com.srh.cinepass.controller;

import com.srh.cinepass.dto.LoginRequest;
import com.srh.cinepass.dto.LoginResponse;
import com.srh.cinepass.dto.SignupRequest;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.AuthService;

<<<<<<< HEAD
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
=======
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

>>>>>>> origin/main
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

<<<<<<< HEAD
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            User user = authService.signup(request);
            return ResponseEntity.ok(new LoginResponse(
                    null, user.getId(), user.getName(), user.getEmail(), user.getRole()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("An account with this email already exists");
        } catch (DataAccessException ex) {
            return databaseUnavailable();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(safeMessage(ex, "Unable to create account"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (DataAccessException ex) {
            return databaseUnavailable();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new LoginResponse(
                null, user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }

    private ResponseEntity<String> databaseUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("The booking service is temporarily unavailable. Please try again shortly.");
    }

    private String safeMessage(RuntimeException ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
=======
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
>>>>>>> origin/main
