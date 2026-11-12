
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

        // CREATE BOOKING
        @Transactional
        public Booking createBooking(BookingRequest request) {

                // 1. Validate request.
                if (request == null) {
                        throw new IllegalArgumentException("Booking request is required");
                }

                if (request.getShowId() == null) {
                        throw new IllegalArgumentException("Show ID is required");
                }

                if (request.getSeatIds() == null ||
                                request.getSeatIds().isEmpty()) {

                        throw new IllegalArgumentException(
                                        "At least one seat must be selected");
                }

                if (request.getSeatIds().size() > 5) {
                        throw new IllegalArgumentException(
                                        "Maximum 5 seats can be booked");
                }

                if (request.getUserId() == null ||
                                request.getUserId().isBlank()) {

                        throw new IllegalArgumentException("User ID is required");
                }

                // 2. Find show.
                Show show = showRepository.findById(request.getShowId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Show not found with ID: " + request.getShowId()));

                // 3. Prevent duplicate seat IDs.
                Set<Long> uniqueSeatIds = new HashSet<>(request.getSeatIds());

                if (uniqueSeatIds.size() != request.getSeatIds().size()) {
                        throw new IllegalArgumentException(
                                        "Duplicate seats are not allowed");
                }

                // 4. Sort IDs to maintain consistent lock order.
                List<Long> sortedSeatIds = new ArrayList<>(uniqueSeatIds);
                Collections.sort(sortedSeatIds);

                // 5. Lock and validate every selected seat.
                List<Seat> selectedSeats = new ArrayList<>();

                for (Long seatId : sortedSeatIds) {

                        if (seatId == null) {
                                throw new IllegalArgumentException("Invalid seat ID");
                        }

                        Seat seat = seatRepository.findByIdForUpdate(seatId)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Seat not found with ID: " + seatId));

                        // Make sure the seat belongs to the selected show.
                        if (seat.getShow() == null ||
                                        !seat.getShow().getId().equals(show.getId())) {

                                throw new IllegalArgumentException(
                                                "Seat " + seat.getSeatNumber()
                                                                + " does not belong to this show");
                        }

                        // Make sure the seat is still available.
                        if (seat.isBooked()) {
                                throw new IllegalArgumentException(
                                                "Seat " + seat.getSeatNumber()
                                                                + " is already booked");
                        }

                        selectedSeats.add(seat);
                }

                // 6. Mark all selected seats as booked.
                List<String> seatNumbers = new ArrayList<>();

                for (Seat seat : selectedSeats) {
                        seat.setBooked(true);
                        seatNumbers.add(seat.getSeatNumber());
                }

                // Flush seat changes within the current transaction.
                seatRepository.saveAllAndFlush(selectedSeats);

                // 7. Calculate total amount on the server.
                int numberOfSeats = selectedSeats.size();

                double totalAmount = numberOfSeats * show.getTicketPrice();

                // 8. Create booking.
                Booking booking = new Booking();

                booking.setUserId(request.getUserId());
                booking.setUserEmail(request.getUserEmail());
                booking.setShow(show);
                booking.setSeatNumbers(seatNumbers);
                booking.setNumberOfSeats(numberOfSeats);
                booking.setTotalAmount(totalAmount);
                booking.setStatus("CONFIRMED");
                booking.setBookingDate(LocalDateTime.now());

                // 9. Save booking.
                // If saving fails, the transaction rolls back seat changes too.
                return bookingRepository.saveAndFlush(booking);
        }

        // GET BOOKING BY ID
        @Transactional(readOnly = true)
        public Booking getBookingById(Long id) {

                return bookingRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Booking not found with ID: " + id));
        }

        // GET BOOKINGS BY USER
        @Transactional(readOnly = true)
        public List<Booking> getBookingsByUser(String userId) {

                return bookingRepository.findByUserId(userId);
        }

        // GET ALL BOOKINGS
        @Transactional(readOnly = true)
        public List<Booking> getAllBookings() {

                return bookingRepository.findAll();
        }

        // GET BOOKINGS BY SHOW
        @Transactional(readOnly = true)
        public List<Booking> getBookingsByShow(Long showId) {

                return bookingRepository.findByShowId(showId);
        }

        // GET SEATS BY SHOW
        @Transactional(readOnly = true)
        public List<Seat> getSeatsByShow(Long showId) {

                if (!showRepository.existsById(showId)) {
                        throw new IllegalArgumentException(
                                        "Show not found with ID: " + showId);
                }

                return seatRepository.findByShowIdOrderByIdAsc(showId);
        }
}
