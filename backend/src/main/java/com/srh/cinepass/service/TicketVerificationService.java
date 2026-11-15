package com.srh.cinepass.service;

import com.srh.cinepass.dto.TicketVerificationResponse;
import com.srh.cinepass.dto.VerificationResult;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.repository.TicketRepository;
import com.srh.cinepass.util.SeatLabels;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketVerificationService {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final SecureTicketTokenService tokenService;
    private final Clock clock;
    private final int admissionOpensMinutesBefore;
    private final int admissionClosesMinutesAfter;

    public TicketVerificationService(
            TicketRepository ticketRepository,
            TicketService ticketService,
            SecureTicketTokenService tokenService,
            Clock clock,
            @Value("${app.tickets.admission-opens-minutes-before:120}") int admissionOpensMinutesBefore,
            @Value("${app.tickets.admission-closes-minutes-after:30}") int admissionClosesMinutesAfter) {

        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
        this.tokenService = tokenService;
        this.clock = clock;
        this.admissionOpensMinutesBefore = Math.max(0, admissionOpensMinutesBefore);
        this.admissionClosesMinutesAfter = Math.max(0, admissionClosesMinutesAfter);
    }

    /**
     * Atomic admission. The ticket row is locked for the duration of this
     * transaction, so concurrent scans cannot both mark the same ticket USED.
     */
    @Transactional
    public TicketVerificationResponse verify(String rawToken, User staff) {
        if (staff == null) {
            throw new TicketAccessDeniedException();
        }

        String token = rawToken == null ? "" : rawToken.trim();

        if (!tokenService.isWellFormedToken(token)) {
            return invalid();
        }

        Ticket ticket = ticketRepository.findByVerificationTokenForUpdate(token)
                .orElse(null);

        if (ticket == null) {
            return invalid();
        }

        Booking booking = ticket.getBooking();
        LocalDateTime now = LocalDateTime.now(clock);

        if (ticket.getStatus() == TicketStatus.USED) {
            return response(
                    ticket,
                    VerificationResult.ALREADY_USED,
                    "Ticket Already Used",
                    false,
                    ticket.getVerifiedAt(),
                    ticket.getVerifiedBy() != null ? ticket.getVerifiedBy().getName() : null);
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED
                || (booking != null && "CANCELLED".equalsIgnoreCase(booking.getStatus()))) {

            if (ticket.getStatus() == TicketStatus.BOOKED) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticket.setCancelledAt(now);
                ticketRepository.saveAndFlush(ticket);
            }

            return response(
                    ticket,
                    VerificationResult.CANCELLED,
                    "Booking Cancelled",
                    false,
                    null,
                    null);
        }

        if (ticket.getStatus() == TicketStatus.EXPIRED) {
            return response(
                    ticket,
                    VerificationResult.EXPIRED,
                    "Ticket Expired",
                    false,
                    null,
                    null);
        }

        if (booking == null
                || !"CONFIRMED".equalsIgnoreCase(booking.getStatus())
                || !"PAID".equalsIgnoreCase(booking.getPaymentStatus())) {

            return response(
                    ticket,
                    VerificationResult.NOT_CONFIRMED,
                    "Booking Not Confirmed",
                    false,
                    null,
                    null);
        }

        LocalDateTime showStart = ticketService.showStartTime(booking);

        if (showStart == null) {
            return invalid();
        }

        if (now.isBefore(showStart.minusMinutes(admissionOpensMinutesBefore))) {
            return response(
                    ticket,
                    VerificationResult.NOT_YET_VALID,
                    "Ticket Is Not Valid Yet",
                    false,
                    null,
                    null);
        }

        if (now.isAfter(showStart.plusMinutes(admissionClosesMinutesAfter))) {
            ticket.setStatus(TicketStatus.EXPIRED);
            ticketRepository.saveAndFlush(ticket);

            return response(
                    ticket,
                    VerificationResult.EXPIRED,
                    "Ticket Expired",
                    false,
                    null,
                    null);
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setVerifiedAt(now);
        ticket.setVerifiedBy(staff);

        Ticket saved = ticketRepository.saveAndFlush(ticket);

        return response(
                saved,
                VerificationResult.VALID,
                "Ticket Verified Successfully",
                true,
                saved.getVerifiedAt(),
                staff.getName());
    }

    private TicketVerificationResponse invalid() {
        return new TicketVerificationResponse(
                VerificationResult.INVALID.name(),
                "Invalid Ticket",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private TicketVerificationResponse response(
            Ticket ticket,
            VerificationResult result,
            String message,
            boolean includeCustomerName,
            LocalDateTime verifiedAt,
            String verifiedBy) {

        Booking booking = ticket.getBooking();

        List<String> seats = booking != null && booking.getSeatNumbers() != null
                ? booking.getSeatNumbers().stream().map(SeatLabels::toDisplayLabel).toList()
                : List.of();

        return new TicketVerificationResponse(
                result.name(),
                message,
                ticket.getTicketReference(),
                booking != null && booking.getShow() != null && booking.getShow().getMovie() != null
                        ? booking.getShow().getMovie().getTitle()
                        : null,
                booking != null && booking.getShow() != null && booking.getShow().getTheatre() != null
                        ? booking.getShow().getTheatre().getName()
                        : null,
                booking != null && booking.getShow() != null
                        ? booking.getShow().getShowDate()
                        : null,
                booking != null && booking.getShow() != null
                        ? booking.getShow().getShowTime()
                        : null,
                seats,
                booking != null && booking.getNumberOfSeats() != null
                        ? booking.getNumberOfSeats()
                        : seats.size(),
                includeCustomerName ? ticketService.customerName(booking) : null,
                verifiedAt,
                verifiedBy,
                booking != null ? booking.getStatus() : null);
    }
}
