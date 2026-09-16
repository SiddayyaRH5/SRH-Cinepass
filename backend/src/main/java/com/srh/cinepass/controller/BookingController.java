package com.srh.cinepass.controller;

import com.srh.cinepass.dto.BookingRequest;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // =========================================================
    // CREATE BOOKING
    // =========================================================

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest request,
            Authentication authentication) {

        try {

            // Get currently logged-in user from JWT
            User loggedInUser = (User) authentication.getPrincipal();

            // Automatically use logged-in user's information
            request.setUserId(
                    String.valueOf(loggedInUser.getId()));

            request.setUserEmail(
                    loggedInUser.getEmail());

            // Create booking
            Booking booking = bookingService.createBooking(request);

            return ResponseEntity.ok(booking);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // GET BOOKING BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id) {

        try {

            Booking booking = bookingService.getBookingById(id);

            return ResponseEntity.ok(booking);

        } catch (Exception e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // =========================================================
    // GET BOOKINGS BY USER ID
    // =========================================================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getBookingsByUser(
            @PathVariable String userId) {

        return ResponseEntity.ok(
                bookingService.getBookingsByUser(userId));
    }

    // =========================================================
    // GET MY BOOKINGS
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<List<Booking>> getMyBookings(
            Authentication authentication) {

        // Get currently logged-in user from JWT
        User loggedInUser = (User) authentication.getPrincipal();

        // Get logged-in user's ID
        String userId = String.valueOf(loggedInUser.getId());

        // Return only this user's bookings
        return ResponseEntity.ok(
                bookingService.getBookingsByUser(userId));
    }

    // =========================================================
    // GET ALL BOOKINGS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {

        return ResponseEntity.ok(
                bookingService.getAllBookings());
    }

    // =========================================================
    // GET BOOKINGS FOR A SHOW
    // =========================================================

    @GetMapping("/show/{showId}")
    public ResponseEntity<List<Booking>> getBookingsByShow(
            @PathVariable Long showId) {

        return ResponseEntity.ok(
                bookingService.getBookingsByShow(showId));
    }
}