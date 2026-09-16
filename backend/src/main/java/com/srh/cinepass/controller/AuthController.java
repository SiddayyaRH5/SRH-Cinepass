package com.srh.cinepass.controller;

import com.srh.cinepass.dto.LoginRequest;
import com.srh.cinepass.dto.LoginResponse;
import com.srh.cinepass.dto.SignupRequest;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================
    // SIGNUP
    // =========================

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody SignupRequest request) {

        try {

            User user = authService.signup(request);

            return ResponseEntity.ok(user);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            LoginResponse response = authService.login(request);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}