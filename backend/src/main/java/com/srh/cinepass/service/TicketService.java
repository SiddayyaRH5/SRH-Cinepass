package com.srh.cinepass.service;

import com.srh.cinepass.dto.TicketResponse;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.exception.TicketNotFoundException;
import com.srh.cinepass.exception.TicketOperationException;
import com.srh.cinepass.repository.BookingRepository;
import com.srh.cinepass.repository.TicketRepository;
import com.srh.cinepass.repository.UserRepository;
import com.srh.cinepass.util.SeatLabels;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SecureTicketTokenService tokenService;
    private final QrCodeService qrCodeService;
    private final Clock clock;
    private final int admissionClosesMinutesAfter;

    public TicketService(
            TicketRepository ticketRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository,
            SecureTicketTokenService tokenService,
            QrCodeService qrCodeService,
            Clock clock,
            @Value("${app.tickets.admission-closes-minutes-after:30}") int admissionClosesMinutesAfter) {

        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.qrCodeService = qrCodeService;
        this.clock = clock;
        this.admissionClosesMinutesAfter = Math.max(0, admissionClosesMinutesAfter);
    }

    /**
     * Issues exactly one ticket for a paid, confirmed booking. Called from the
     * existing payment-confirmation transaction. The unique booking_id
     * constraint and the booking row lock make retries idempotent.
     */
    @Transactional
    public Ticket issueForConfirmedBooking(Booking booking) {
        if (booking == null || booking.getId() == null) {
            throw new TicketOperationException("A saved booking is required before issuing a ticket");
        }

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())
                || !"PAID".equalsIgnoreCase(booking.getPaymentStatus())) {
            throw new TicketOperationException(
                    "Tickets can only be issued for paid, confirmed bookings");
        }

        Ticket existing = ticketRepository.findByBookingId(booking.getId()).orElse(null);

        if (existing != null) {
            return existing;
        }

        String verificationToken = null;
        String ticketReference = null;

        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String candidateToken = tokenService.newVerificationToken();
            String candidateReference = tokenService.newTicketReference();

            if (!ticketRepository.existsByVerificationToken(candidateToken)
                    && !ticketRepository.existsByTicketReference(candidateReference)) {
                verificationToken = candidateToken;
                ticketReference = candidateReference;
                break;
            }
        }

        if (verificationToken == null || ticketReference == null) {
            throw new TicketOperationException("A unique ticket credential could not be generated");
        }

        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setVerificationToken(verificationToken);
        ticket.setTicketReference(ticketReference);
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setCreatedAt(LocalDateTime.now(clock));

        return ticketRepository.saveAndFlush(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsForCustomer(User user) {
        requireUser(user);

        return ticketRepository.findAllDetailedForUser(String.valueOf(user.getId()))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketForCustomer(Long ticketId, User user) {
        Ticket ticket = findAuthorizedTicket(ticketId, user);
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public byte[] getQrCodePng(Long ticketId, User user) {
        Ticket ticket = findAuthorizedTicket(ticketId, user);
        return qrCodeService.generatePng(ticket.getVerificationToken());
    }

    @Transactional
    public int cancelTicketsForBooking(Long bookingId) {
        if (bookingId == null) {
            return 0;
        }

        return ticketRepository.cancelUnusedForBooking(
                bookingId,
                TicketStatus.CANCELLED,
                TicketStatus.BOOKED,
                LocalDateTime.now(clock));
    }

    /**
     * Used by the startup backfill for confirmed bookings created before this
     * feature existed. Safe to call repeatedly.
     */
    @Transactional
    public int backfillMissingTickets() {
        List<Long> bookingIds = bookingRepository.findConfirmedBookingIdsWithoutTicket();
        int issued = 0;

        for (Long bookingId : bookingIds) {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);

            if (booking == null) {
                continue;
            }

            issueForConfirmedBooking(booking);
            issued++;
        }

        return issued;
    }

    public TicketStatus effectiveStatus(Ticket ticket, LocalDateTime now) {
        if (ticket == null) {
            return null;
        }

        if (ticket.getStatus() == TicketStatus.USED
                || ticket.getStatus() == TicketStatus.CANCELLED
                || ticket.getStatus() == TicketStatus.EXPIRED) {
            return ticket.getStatus();
        }

        Booking booking = ticket.getBooking();

        if (booking != null && "CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            return TicketStatus.CANCELLED;
        }

        LocalDateTime showStart = showStartTime(booking);

        if (showStart != null && now.isAfter(showStart.plusMinutes(admissionClosesMinutesAfter))) {
            return TicketStatus.EXPIRED;
        }

        return TicketStatus.BOOKED;
    }

    public LocalDateTime showStartTime(Booking booking) {
        if (booking == null
                || booking.getShow() == null
                || booking.getShow().getShowDate() == null
                || booking.getShow().getShowTime() == null) {
            return null;
        }

        return LocalDateTime.of(
                booking.getShow().getShowDate(),
                booking.getShow().getShowTime());
    }

    public String customerName(Booking booking) {
        if (booking == null || booking.getUserId() == null) {
            return null;
        }

        try {
            Long userId = Long.valueOf(booking.getUserId());

            return userRepository.findById(userId)
                    .map(User::getName)
                    .orElse(null);

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public TicketResponse toResponse(Ticket ticket) {
        Booking booking = ticket.getBooking();
        LocalDateTime now = LocalDateTime.now(clock);

        List<String> seats = new ArrayList<>();

        if (booking != null && booking.getSeatNumbers() != null) {
            booking.getSeatNumbers()
                    .stream()
                    .map(SeatLabels::toDisplayLabel)
                    .forEach(seats::add);
        }

        return new TicketResponse(
                ticket.getId(),
                booking != null ? booking.getId() : null,
                ticket.getTicketReference(),
                effectiveStatus(ticket, now).name(),
                booking != null ? booking.getStatus() : null,
                movieTitle(booking),
                moviePosterUrl(booking),
                customerName(booking),
                theatreName(booking),
                theatreCity(booking),
                null,
                showDate(booking),
                showTime(booking),
                seats,
                booking != null && booking.getNumberOfSeats() != null
                        ? booking.getNumberOfSeats()
                        : seats.size(),
                booking != null ? booking.getBookingDate() : null,
                ticket.getCreatedAt(),
                ticket.getVerifiedAt());
    }

    private Ticket findAuthorizedTicket(Long ticketId, User user) {
        requireUser(user);

        Ticket ticket = ticketRepository.findDetailedById(ticketId)
                .orElseThrow(TicketNotFoundException::new);

        boolean owner = ticket.getBooking() != null
                && String.valueOf(user.getId()).equals(ticket.getBooking().getUserId());

        boolean admin = "ADMIN".equalsIgnoreCase(user.getRole());

        if (!owner && !admin) {
            throw new TicketAccessDeniedException();
        }

        return ticket;
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new TicketAccessDeniedException();
        }
    }

    private String movieTitle(Booking booking) {
        if (booking == null || booking.getShow() == null || booking.getShow().getMovie() == null) {
            return null;
        }

        return booking.getShow().getMovie().getTitle();
    }

    private String moviePosterUrl(Booking booking) {
        if (booking == null || booking.getShow() == null || booking.getShow().getMovie() == null) {
            return null;
        }

        return booking.getShow().getMovie().getPosterUrl();
    }

    private String theatreName(Booking booking) {
        if (booking == null || booking.getShow() == null || booking.getShow().getTheatre() == null) {
            return null;
        }

        return booking.getShow().getTheatre().getName();
    }

    private String theatreCity(Booking booking) {
        if (booking == null
                || booking.getShow() == null
                || booking.getShow().getTheatre() == null
                || booking.getShow().getTheatre().getLocation() == null) {
            return null;
        }

        return booking.getShow().getTheatre().getLocation().getCity();
    }

    private java.time.LocalDate showDate(Booking booking) {
        if (booking == null || booking.getShow() == null) {
            return null;
        }

        return booking.getShow().getShowDate();
    }

    private java.time.LocalTime showTime(Booking booking) {
        if (booking == null || booking.getShow() == null) {
            return null;
        }

        return booking.getShow().getShowTime();
    }
}
