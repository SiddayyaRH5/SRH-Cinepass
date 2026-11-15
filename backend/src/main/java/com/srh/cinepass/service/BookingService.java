package com.srh.cinepass.service;

import com.srh.cinepass.dto.BookingRequest;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.repository.BookingRepository;
import com.srh.cinepass.repository.SeatRepository;
import com.srh.cinepass.repository.ShowRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

        private static final int RESERVATION_MINUTES = 10;

        private static final String STATUS_PENDING = "PENDING";
        private static final String STATUS_CONFIRMED = "CONFIRMED";
        private static final String STATUS_CANCELLED = "CANCELLED";
        public static final String STATUS_PAYMENT_REVIEW = "PAYMENT_REVIEW";

        private static final String PAYMENT_PENDING = "PENDING";
        private static final String PAYMENT_PAID = "PAID";
        private static final String PAYMENT_FAILED = "FAILED";

        public static final String RECOVERY_MANUAL_REVIEW = "MANUAL_REVIEW";
        public static final String RECOVERY_RECOVERED = "RECOVERED";

        private final BookingRepository bookingRepository;
        private final ShowRepository showRepository;
        private final SeatRepository seatRepository;
        private final TicketService ticketService;
        private final Clock clock;

        public BookingService(
                        BookingRepository bookingRepository,
                        ShowRepository showRepository,
                        SeatRepository seatRepository,
                        TicketService ticketService,
                        Clock clock) {

                this.bookingRepository = bookingRepository;
                this.showRepository = showRepository;
                this.seatRepository = seatRepository;
                this.ticketService = ticketService;
                this.clock = clock;
        }

        // Create a pending booking and temporarily reserve its seats.
        @Transactional
        public Booking createPendingBooking(BookingRequest request) {

                if (request == null) {
                        throw new IllegalArgumentException("Booking request is required");
                }

                if (request.getShowId() == null) {
                        throw new IllegalArgumentException("Show ID is required");
                }

                if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
                        throw new IllegalArgumentException("At least one seat must be selected");
                }

                if (request.getSeatIds().size() > 5) {
                        throw new IllegalArgumentException("Maximum 5 seats can be booked");
                }

                if (request.getUserId() == null || request.getUserId().isBlank()) {
                        throw new IllegalArgumentException("User ID is required");
                }

                Show show = showRepository.findById(request.getShowId())
                                .orElseThrow(() -> new IllegalArgumentException("Show not found"));

                Set<Long> uniqueSeatIds = new HashSet<>(request.getSeatIds());

                if (uniqueSeatIds.size() != request.getSeatIds().size()) {
                        throw new IllegalArgumentException("Duplicate seats are not allowed");
                }

                List<Long> sortedSeatIds = new ArrayList<>(uniqueSeatIds);
                Collections.sort(sortedSeatIds);

                List<Seat> selectedSeats = new ArrayList<>();
                List<String> seatNumbers = new ArrayList<>();

                LocalDateTime now = LocalDateTime.now(clock);

                // Lock seats in a consistent order to avoid concurrent booking conflicts.
                for (Long seatId : sortedSeatIds) {

                        if (seatId == null) {
                                throw new IllegalArgumentException("Invalid seat ID");
                        }

                        Seat seat = seatRepository.findByIdForUpdate(seatId)
                                        .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

                        if (seat.getShow() == null ||
                                        !seat.getShow().getId().equals(show.getId())) {
                                throw new IllegalArgumentException(
                                                "Seat " + seat.getSeatNumber() + " does not belong to this show");
                        }

                        if (seat.isBookedAt(now)) {
                                throw new IllegalArgumentException(
                                                "Seat " + seat.getSeatNumber() + " is already booked or reserved");
                        }

                        // Clear expired reservation metadata before assigning a new reservation.
                        seat.setReservedByBookingId(null);
                        seat.setReservedUntil(null);

                        selectedSeats.add(seat);
                        seatNumbers.add(seat.getSeatNumber());
                }

                double totalAmount = selectedSeats.size() * show.getTicketPrice();

                Booking booking = new Booking();
                booking.setUserId(request.getUserId());
                booking.setUserEmail(request.getUserEmail());
                booking.setShow(show);
                booking.setSeatNumbers(seatNumbers);
                booking.setNumberOfSeats(selectedSeats.size());
                booking.setTotalAmount(totalAmount);
                booking.setStatus(STATUS_PENDING);
                booking.setPaymentStatus(PAYMENT_PENDING);
                booking.setBookingDate(now);
                booking.setReservationExpiresAt(now.plusMinutes(RESERVATION_MINUTES));

                // Save first so the booking has an ID for the seat reservations.
                booking = bookingRepository.saveAndFlush(booking);

                for (Seat seat : selectedSeats) {
                        seat.setReservedByBookingId(booking.getId());
                        seat.setReservedUntil(booking.getReservationExpiresAt());
                }

                seatRepository.saveAllAndFlush(selectedSeats);

                return booking;
        }

        // Save the Razorpay order ID after the external order request succeeds.
        @Transactional
        public Booking attachRazorpayOrder(Long bookingId, String orderId) {

                Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Booking not found with ID: " + bookingId));

                if (!STATUS_PENDING.equals(booking.getStatus())) {
                        throw new IllegalStateException("Booking is no longer pending");
                }

                if (booking.getReservationExpiresAt() == null ||
                                !booking.getReservationExpiresAt().isAfter(LocalDateTime.now(clock))) {
                        throw new IllegalStateException("Seat reservation has expired");
                }

                booking.setRazorpayOrderId(orderId);

                return bookingRepository.save(booking);
        }

        @Transactional(readOnly = true)
        public Booking getBookingByOrderId(String orderId) {
                return bookingRepository.findByRazorpayOrderId(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found for this order"));
        }

        /*
         * Release seats if Razorpay order creation fails or payment is cancelled.
         * A booking that already has a captured payment is never marked FAILED:
         * it is parked for manual review so the captured money is preserved.
         */
        @Transactional
        public Booking cancelPendingBooking(Long bookingId) {

                Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Booking not found with ID: " + bookingId));

                if (STATUS_CONFIRMED.equals(booking.getStatus())
                                || STATUS_PAYMENT_REVIEW.equals(booking.getStatus())) {
                        return booking;
                }

                if (PAYMENT_PAID.equalsIgnoreCase(booking.getPaymentStatus())
                                || booking.getRazorpayPaymentId() != null) {

                        return markPaymentForManualReview(
                                        booking,
                                        booking.getRazorpayPaymentId(),
                                        "Cancellation was requested after payment capture; manual review required",
                                        LocalDateTime.now(clock));
                }

                releaseSeats(booking);

                booking.setStatus(STATUS_CANCELLED);
                booking.setPaymentStatus(PAYMENT_FAILED);
                booking.setReservationExpiresAt(null);

                bookingRepository.save(booking);

                // Retained ticket history is invalidated, never deleted.
                ticketService.cancelTicketsForBooking(booking.getId());

                return booking;
        }

        /**
         * Confirm a booking only after successful server-side payment verification.
         *
         * <p>If the original ten-minute reservation has expired (or a pending
         * cancellation released it) but the captured payment is genuine, the
         * booking is recovered when the originally selected seats are still
         * available. Seats that were resold or reserved by another booking are
         * never stolen; the booking is moved to an explicit manual-review state
         * and the captured payment is retained.</p>
         */
        @Transactional
        public Booking confirmPaidBooking(String orderId, String paymentId) {

                Booking booking = bookingRepository.findByRazorpayOrderIdForUpdate(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Booking for this order was not found"));

                // Make repeated verification requests safe.
                if (STATUS_CONFIRMED.equals(booking.getStatus())) {

                        if (PAYMENT_PAID.equalsIgnoreCase(booking.getPaymentStatus())) {
                                ticketService.issueForConfirmedBooking(booking);
                        }

                        return booking;
                }

                if (STATUS_PAYMENT_REVIEW.equals(booking.getStatus())) {
                        return booking;
                }

                LocalDateTime now = LocalDateTime.now(clock);

                List<Seat> seats;

                try {
                        seats = findAndLockBookingSeats(booking);
                } catch (IllegalStateException missingSeats) {
                        return markPaymentForManualReview(
                                        booking,
                                        paymentId,
                                        "The original seat records are no longer available; manual review required",
                                        now);
                }

                boolean reservationValid = STATUS_PENDING.equals(booking.getStatus())
                                && booking.getReservationExpiresAt() != null
                                && booking.getReservationExpiresAt().isAfter(now);

                if (reservationValid) {
                        for (Seat seat : seats) {
                                if (seat.getReservedByBookingId() == null
                                                || !seat.getReservedByBookingId().equals(booking.getId())
                                                || seat.getReservedUntil() == null
                                                || !seat.getReservedUntil().isAfter(now)) {

                                        reservationValid = false;
                                        break;
                                }
                        }
                }

                /*
                 * Check availability regardless of whether our own reservation
                 * is still valid. A seat that has already been sold to another
                 * customer must never be reassigned to this booking.
                 */
                String unavailableSeat = firstUnavailableSeat(seats, booking, now);

                if (unavailableSeat != null) {
                        releaseOwnReservations(seats, booking);

                        return markPaymentForManualReview(
                                        booking,
                                        paymentId,
                                        "Seat " + unavailableSeat
                                                        + " is no longer available; manual refund or reassignment required",
                                        now);
                }

                for (Seat seat : seats) {
                        seat.setBooked(true);
                        seat.setReservedByBookingId(null);
                        seat.setReservedUntil(null);
                }

                seatRepository.saveAllAndFlush(seats);

                booking.setRazorpayPaymentId(paymentId);
                booking.setPaymentStatus(PAYMENT_PAID);
                booking.setStatus(STATUS_CONFIRMED);
                booking.setReservationExpiresAt(null);

                if (!reservationValid) {
                        booking.setPaymentRecoveryStatus(RECOVERY_RECOVERED);
                        booking.setPaymentRecoveryReason(
                                        "Captured payment confirmed after the seat reservation expired");
                        booking.setPaymentRecoveryAt(now);
                }

                Booking saved = bookingRepository.saveAndFlush(booking);

                /*
                 * Issue the ticket inside the same transaction. If ticket
                 * issuance fails, the booking confirmation is rolled back and
                 * the idempotent payment-verification call can be retried.
                 */
                ticketService.issueForConfirmedBooking(saved);

                return saved;
        }

        @Transactional(readOnly = true)
        public List<Booking> getBookingsRequiringPaymentReview() {
                return bookingRepository.findByPaymentRecoveryStatusOrderByBookingDateDesc(
                                RECOVERY_MANUAL_REVIEW);
        }

        private Booking markPaymentForManualReview(
                        Booking booking,
                        String paymentId,
                        String reason,
                        LocalDateTime now) {

                if (paymentId != null && !paymentId.isBlank()) {
                        booking.setRazorpayPaymentId(paymentId);
                }

                booking.setPaymentStatus(PAYMENT_PAID);
                booking.setStatus(STATUS_PAYMENT_REVIEW);
                booking.setPaymentRecoveryStatus(RECOVERY_MANUAL_REVIEW);
                booking.setPaymentRecoveryReason(reason);
                booking.setPaymentRecoveryAt(now);
                booking.setReservationExpiresAt(null);

                return bookingRepository.saveAndFlush(booking);
        }

        private String firstUnavailableSeat(
                        List<Seat> seats,
                        Booking booking,
                        LocalDateTime now) {

                for (Seat seat : seats) {

                        if (seat.isPermanentlyBooked()) {
                                return seat.getSeatNumber();
                        }

                        boolean reservedByAnotherBooking = seat.getReservedByBookingId() != null
                                        && !seat.getReservedByBookingId().equals(booking.getId())
                                        && seat.getReservedUntil() != null
                                        && seat.getReservedUntil().isAfter(now);

                        if (reservedByAnotherBooking) {
                                return seat.getSeatNumber();
                        }
                }

                return null;
        }

        private void releaseOwnReservations(List<Seat> seats, Booking booking) {

                List<Seat> released = new ArrayList<>();

                for (Seat seat : seats) {
                        if (booking.getId().equals(seat.getReservedByBookingId())) {
                                seat.setReservedByBookingId(null);
                                seat.setReservedUntil(null);
                                released.add(seat);
                        }
                }

                if (!released.isEmpty()) {
                        seatRepository.saveAllAndFlush(released);
                }
        }

        private List<Seat> findAndLockBookingSeats(Booking booking) {

                List<Seat> allSeats = seatRepository.findByShowIdOrderByIdAsc(
                                booking.getShow().getId());

                List<Long> seatIds = new ArrayList<>();

                for (Seat seat : allSeats) {
                        if (booking.getSeatNumbers().contains(seat.getSeatNumber())) {
                                seatIds.add(seat.getId());
                        }
                }

                Collections.sort(seatIds);

                if (seatIds.size() != booking.getSeatNumbers().size()) {
                        throw new IllegalStateException("Could not find all reserved seats");
                }

                List<Seat> lockedSeats = new ArrayList<>();

                for (Long seatId : seatIds) {
                        Seat seat = seatRepository.findByIdForUpdate(seatId)
                                        .orElseThrow(() -> new IllegalStateException("Reserved seat no longer exists"));
                        lockedSeats.add(seat);
                }

                return lockedSeats;
        }

        private void releaseSeats(Booking booking) {

                List<Seat> allSeats = seatRepository.findByShowIdOrderByIdAsc(
                                booking.getShow().getId());

                List<Long> seatIds = new ArrayList<>();

                for (Seat seat : allSeats) {
                        if (booking.getSeatNumbers().contains(seat.getSeatNumber())) {
                                seatIds.add(seat.getId());
                        }
                }

                Collections.sort(seatIds);

                List<Seat> lockedSeats = new ArrayList<>();

                for (Long seatId : seatIds) {
                        Seat seat = seatRepository.findByIdForUpdate(seatId)
                                        .orElseThrow(() -> new IllegalStateException("Reserved seat no longer exists"));

                        if (seat.getReservedByBookingId() != null &&
                                        seat.getReservedByBookingId().equals(booking.getId())) {

                                seat.setReservedByBookingId(null);
                                seat.setReservedUntil(null);
                                lockedSeats.add(seat);
                        }
                }

                seatRepository.saveAllAndFlush(lockedSeats);
        }

        // Disable the old direct-confirmation booking method.
        // All new bookings must go through Razorpay.
        @Transactional
        public Booking createBooking(BookingRequest request) {
                throw new IllegalStateException(
                                "Direct booking is disabled. Payment is required.");
        }

        @Transactional(readOnly = true)
        public Booking getBookingById(Long id) {

                return bookingRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));
        }

        @Transactional(readOnly = true)
        public List<Booking> getBookingsByUser(String userId) {
                return bookingRepository.findByUserId(userId);
        }

        @Transactional(readOnly = true)
        public List<Booking> getAllBookings() {
                return bookingRepository.findAll();
        }

        @Transactional(readOnly = true)
        public List<Booking> getBookingsByShow(Long showId) {
                return bookingRepository.findByShowId(showId);
        }

        @Transactional(readOnly = true)
        public List<Seat> getSeatsByShow(Long showId) {

                if (!showRepository.existsById(showId)) {
                        throw new IllegalArgumentException("Show not found with ID: " + showId);
                }

                return seatRepository.findByShowIdOrderByIdAsc(showId);
        }
}
