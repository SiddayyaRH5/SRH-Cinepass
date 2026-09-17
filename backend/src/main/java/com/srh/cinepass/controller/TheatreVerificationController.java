package com.srh.cinepass.controller;

import com.srh.cinepass.dto.TheatreVerificationRequest;
import com.srh.cinepass.entity.TheatreVerification;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.TheatreVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/theatre-verification")
@CrossOrigin(origins = "http://localhost:5173")
public class TheatreVerificationController {

    private final TheatreVerificationService verificationService;

    public TheatreVerificationController(
            TheatreVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody TheatreVerificationRequest request,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();

            return ResponseEntity.ok(
                    verificationService.submit(request, user));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyVerification(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                verificationService.getMyVerification(user));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<TheatreVerification>>
    getPendingApplications() {

        return ResponseEntity.ok(
                verificationService.getPendingApplications());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {

        try {
            return ResponseEntity.ok(
                    verificationService.approve(id));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        try {
            return ResponseEntity.ok(
                    verificationService.reject(
                            id,
                            body.get("reason")));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
}