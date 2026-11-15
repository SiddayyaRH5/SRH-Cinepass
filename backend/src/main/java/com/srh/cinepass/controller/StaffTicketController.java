package com.srh.cinepass.controller;

import com.srh.cinepass.dto.TicketVerificationRequest;
import com.srh.cinepass.dto.TicketVerificationResponse;
import com.srh.cinepass.dto.VerificationResult;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.service.TicketVerificationRateLimiter;
import com.srh.cinepass.service.TicketVerificationService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/tickets")
public class StaffTicketController {

    private final TicketVerificationService verificationService;
    private final TicketVerificationRateLimiter rateLimiter;

    public StaffTicketController(
            TicketVerificationService verificationService,
            TicketVerificationRateLimiter rateLimiter) {

        this.verificationService = verificationService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/verify")
    public ResponseEntity<TicketVerificationResponse> verify(
            @Valid @RequestBody TicketVerificationRequest request,
            Authentication authentication) {

        User staff = currentStaff(authentication);

        rateLimiter.check(staff.getId());

        TicketVerificationResponse result = verificationService.verify(
                request.verificationToken(),
                staff);

        HttpStatus status = VerificationResult.INVALID.name().equals(result.result())
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.OK;

        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(result);
    }

    private User currentStaff(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {

            throw new TicketAccessDeniedException();
        }

        boolean staff = "STAFF".equalsIgnoreCase(user.getRole());
        boolean admin = "ADMIN".equalsIgnoreCase(user.getRole());

        if (!staff && !admin) {
            throw new TicketAccessDeniedException();
        }

        return user;
    }
}
