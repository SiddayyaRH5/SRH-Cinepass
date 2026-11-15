package com.srh.cinepass.controller;

import com.srh.cinepass.dto.TicketResponse;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.service.TicketService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<TicketResponse>> myTickets(Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticketService.getTicketsForCustomer(currentUser(authentication)));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> ticket(
            @PathVariable Long ticketId,
            Authentication authentication) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticketService.getTicketForCustomer(
                        ticketId,
                        currentUser(authentication)));
    }

    /**
     * Authenticated QR image fetch. The customer page fetches this with its
     * JWT and displays it as an object URL, so the token is never exposed as
     * text in the API or in the page source.
     */
    @GetMapping(value = "/{ticketId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrCode(
            @PathVariable Long ticketId,
            Authentication authentication) {

        byte[] png = ticketService.getQrCodePng(
                ticketId,
                currentUser(authentication));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {

            throw new TicketAccessDeniedException();
        }

        return user;
    }
}
