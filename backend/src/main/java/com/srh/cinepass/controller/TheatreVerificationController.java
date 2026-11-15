package com.srh.cinepass.controller;

import com.srh.cinepass.dto.TheatreVerificationRequest;
import com.srh.cinepass.dto.TheatreVerificationResponse;
import com.srh.cinepass.entity.TheatreVerification;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.service.TheatreVerificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/theatre-verification")
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

        User user = currentUser(authentication);

        try {
            return ResponseEntity.ok(
                    TheatreVerificationResponse.from(
                            verificationService.submit(request, user)));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", safeMessage(e)));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<TheatreVerificationResponse> getMyVerification(
            Authentication authentication) {

        User user = currentUser(authentication);

        TheatreVerification verification =
                verificationService.getMyVerification(user);

        return ResponseEntity.ok(
                verification == null
                        ? null
                        : TheatreVerificationResponse.from(verification));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<TheatreVerificationResponse>>
    getPendingApplications(Authentication authentication) {

        requireAdmin(authentication);

        return ResponseEntity.ok(
                verificationService.getPendingApplications()
                        .stream()
                        .map(TheatreVerificationResponse::from)
                        .toList());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            Authentication authentication) {

        requireAdmin(authentication);

        try {
            return ResponseEntity.ok(
                    TheatreVerificationResponse.from(
                            verificationService.approve(id)));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", safeMessage(e)));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        requireAdmin(authentication);

        try {
            return ResponseEntity.ok(
                    TheatreVerificationResponse.from(
                            verificationService.reject(
                                    id,
                                    body.get("reason"))));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", safeMessage(e)));
        }
    }

    private User currentUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {

            throw new TicketAccessDeniedException();
        }

        return user;
    }

    private void requireAdmin(Authentication authentication) {

        User user = currentUser(authentication);

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new TicketAccessDeniedException();
        }
    }

    private String safeMessage(RuntimeException exception) {

        if (exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return "The request could not be completed";
        }

        return exception.getMessage();
    }
}
