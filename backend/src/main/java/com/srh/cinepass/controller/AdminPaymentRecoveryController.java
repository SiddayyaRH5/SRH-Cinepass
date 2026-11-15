package com.srh.cinepass.controller;

import com.srh.cinepass.dto.PaymentRecoveryResponse;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.service.BookingService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes captured payments that need manual intervention. The security chain
 * already restricts {@code /api/admin/**} to ADMIN; the explicit check here is
 * defence in depth for the payment-recovery surface.
 */
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentRecoveryController {

    private final BookingService bookingService;

    public AdminPaymentRecoveryController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/recovery")
    public ResponseEntity<List<PaymentRecoveryResponse>> recoveryQueue(
            Authentication authentication) {

        requireAdmin(authentication);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(bookingService.getBookingsRequiringPaymentReview()
                        .stream()
                        .map(PaymentRecoveryResponse::from)
                        .toList());
    }

    private void requireAdmin(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)
                || !"ADMIN".equalsIgnoreCase(user.getRole())) {

            throw new TicketAccessDeniedException();
        }
    }
}
