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

    // ==========================================
    // CREATE BOOKING
    // ==========================================

    @Transactional
    public Booking createBooking(BookingRequest request) {

        // ------------------------------------------
        // 1. Validate request
        // ------------------------------------------

        if (request.getShowId() == null) {
            throw new RuntimeException("Show ID is required");
        }

        if (request.getSeatIds() == null ||
                request.getSeatIds().isEmpty()) {

            throw new RuntimeException(
                    "At least one seat must be selected");
        }

        if (request.getSeatIds().size() > 5) {
            throw new RuntimeException(
                    "Maximum 5 seats can be booked");
        }

        if (request.getUserId() == null ||
                request.getUserId().isBlank()) {

            throw new RuntimeException(
                    "User ID is required");
        }

        // ------------------------------------------
        // 2. Find show
        // ------------------------------------------

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Show not found with id: "
                                        + request.getShowId()));

        // ------------------------------------------
        // 3. Prevent duplicate seat IDs
        // ------------------------------------------

        Set<Long> uniqueSeatIds =
                new HashSet<>(request.getSeatIds());

        if (uniqueSeatIds.size() != request.getSeatIds().size()) {
            throw new RuntimeException(
                    "Duplicate seats are not allowed");
        }

        // ------------------------------------------
        // 4. Sort IDs
        // ------------------------------------------
        // Sorting helps reduce the possibility of
        // database deadlocks when multiple users
        // book different seats simultaneously.

        List<Long> sortedSeatIds =
                new ArrayList<>(uniqueSeatIds);

        Collections.sort(sortedSeatIds);

        // ------------------------------------------
        // 5. Lock and validate seats
        // ------------------------------------------

        List<Seat> selectedSeats = new ArrayList<>();

        for (Long seatId : sortedSeatIds) {

            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Seat not found with id: "
                                            + seatId));

            // Make sure the seat belongs to this show
            if (seat.getShow() == null ||
                    !seat.getShow().getId()
                            .equals(show.getId())) {

                throw new RuntimeException(
                        "Seat " + seat.getSeatNumber()
                                + " does not belong to this show");
            }

            // Check whether already booked
            if (seat.isBooked()) {

                throw new RuntimeException(
                        "Seat " + seat.getSeatNumber()
                                + " is already booked");
            }

            selectedSeats.add(seat);
        }

        // ------------------------------------------
        // 6. Mark seats as booked
        // ------------------------------------------

        List<String> seatNumbers = new ArrayList<>();

        for (Seat seat : selectedSeats) {

            seat.setBooked(true);

            seatRepository.save(seat);

            seatNumbers.add(seat.getSeatNumber());
        }

        // ------------------------------------------
        // 7. Calculate total
        // ------------------------------------------

        int numberOfSeats = selectedSeats.size();

        double totalAmount =
                numberOfSeats * show.getTicketPrice();

        // ------------------------------------------
        // 8. Create booking
        // ------------------------------------------

        Booking booking = new Booking();

        booking.setUserId(request.getUserId());

        booking.setUserEmail(request.getUserEmail());

        booking.setShow(show);

        booking.setSeatNumbers(seatNumbers);

        booking.setNumberOfSeats(numberOfSeats);

        booking.setTotalAmount(totalAmount);

        booking.setStatus("CONFIRMED");

        booking.setBookingDate(LocalDateTime.now());

        // ------------------------------------------
        // 9. Save booking
        // ------------------------------------------

        return bookingRepository.save(booking);
    }

    // ==========================================
    // GET BOOKING BY ID
    // ==========================================

    public Booking getBookingById(Long id) {

        return bookingRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found with id: " + id));
    }

    // ==========================================
    // GET USER BOOKINGS
    // ==========================================

    public List<Booking> getBookingsByUser(String userId) {

        return bookingRepository.findByUserId(userId);
    }

    // ==========================================
    // GET ALL BOOKINGS
    // ==========================================

    public List<Booking> getAllBookings() {

        return bookingRepository.findAll();
    }

    // ==========================================
    // GET BOOKINGS FOR SHOW
    // ==========================================

    public List<Booking> getBookingsByShow(Long showId) {

        return bookingRepository.findByShowId(showId);
    }
}