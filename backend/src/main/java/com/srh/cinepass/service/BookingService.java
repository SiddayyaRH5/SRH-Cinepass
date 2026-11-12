
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

        private static final int RESERVATION_MINUTES = 10;

        private final BookingRepository bookingRepository;
        private final ShowRepository showRepository;
        private final SeatRepository seatRepository;

        public BookingService(
                        BookingRepository bookingRepository,
                        ShowRepository showRepository,
                        SeatRepository seatRepository) {

                this.bookingRepository = bookingRepository;
                this.showRepository = showRepository;
                this.seatRepository = seatRepository;
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

                LocalDateTime now = LocalDateTime.now();

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

                        if (seat.isBooked()) {
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
                booking.setStatus("PENDING");
                booking.setPaymentStatus("PENDING");
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

                Booking booking = getBookingById(bookingId);

                if (!"PENDING".equals(booking.getStatus())) {
                        throw new IllegalStateException("Booking is no longer pending");
                }

                if (booking.getReservationExpiresAt() == null ||
                                !booking.getReservationExpiresAt().isAfter(LocalDateTime.now())) {
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

        // Release seats if Razorpay order creation fails or payment is cancelled.
        @Transactional
        public void cancelPendingBooking(Long bookingId) {

                Booking booking = getBookingById(bookingId);

                if ("CONFIRMED".equals(booking.getStatus())) {
                        return;
                }

                releaseSeats(booking);

                booking.setStatus("CANCELLED");
                booking.setPaymentStatus("FAILED");
                booking.setReservationExpiresAt(null);

                bookingRepository.save(booking);
        }

        // Confirm a booking only after successful server-side payment verification.
        @Transactional
        public Booking confirmPaidBooking(String orderId, String paymentId) {

                Booking booking = bookingRepository.findByRazorpayOrderId(orderId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Booking for this order was not found"));

                // Make repeated verification requests safe.
                if ("CONFIRMED".equals(booking.getStatus())) {
                        return booking;
                }

                if (!"PENDING".equals(booking.getStatus())) {
                        throw new IllegalStateException("Booking is not pending");
                }

                LocalDateTime now = LocalDateTime.now();

                if (booking.getReservationExpiresAt() == null ||
                                !booking.getReservationExpiresAt().isAfter(now)) {
                        throw new IllegalStateException("Seat reservation has expired");
                }

                List<Seat> seats = findAndLockBookingSeats(booking);

                for (Seat seat : seats) {
                        if (seat.getReservedByBookingId() == null ||
                                        !seat.getReservedByBookingId().equals(booking.getId())) {
                                throw new IllegalStateException(
                                                "Seat reservation is no longer valid: " + seat.getSeatNumber());
                        }

                        if (seat.getReservedUntil() == null ||
                                        !seat.getReservedUntil().isAfter(now)) {
                                throw new IllegalStateException(
                                                "Seat reservation has expired: " + seat.getSeatNumber());
                        }
                }

                for (Seat seat : seats) {
                        seat.setBooked(true);
                        seat.setReservedByBookingId(null);
                        seat.setReservedUntil(null);
                }

                seatRepository.saveAllAndFlush(seats);

                booking.setRazorpayPaymentId(paymentId);
                booking.setPaymentStatus("PAID");
                booking.setStatus("CONFIRMED");
                booking.setReservationExpiresAt(null);

                return bookingRepository.saveAndFlush(booking);
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
