
package com.srh.cinepass.controller;

import com.srh.cinepass.dto.BookingRequest;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.BookingService;
import com.srh.cinepass.service.RazorpayService;

import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final RazorpayService razorpayService;

    public BookingController(
            BookingService bookingService,
            RazorpayService razorpayService) {

        this.bookingService = bookingService;
        this.razorpayService = razorpayService;
    }

    // STEP 1: Create a pending booking and Razorpay order.
    @PostMapping("/create-order")
    public ResponseEntity<?> createPaymentOrder(
            @RequestBody BookingRequest request,
            Authentication authentication) {

        Booking booking = null;

        try {
            User loggedInUser = getLoggedInUser(authentication);

            // Never trust user ID or email sent by the frontend.
            request.setUserId(String.valueOf(loggedInUser.getId()));
            request.setUserEmail(loggedInUser.getEmail());

            // Reserve the selected seats for 10 minutes.
            booking = bookingService.createPendingBooking(request);

            // Convert rupees to paise using decimal arithmetic.
            int amountInPaise = BigDecimal.valueOf(booking.getTotalAmount())
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();

            String receipt = "cinepass_" + booking.getId();

            // Create the order with Razorpay.
            JSONObject order = razorpayService.createOrder(
                    amountInPaise,
                    receipt);

            String orderId = order.getString("id");

            // Store the Razorpay order ID against this booking.
            booking = bookingService.attachRazorpayOrder(
                    booking.getId(),
                    orderId);

            Map<String, Object> response = new HashMap<>();

            response.put("bookingId", booking.getId());
            response.put("keyId", razorpayService.getKeyId());
            response.put("orderId", orderId);
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("name", "SRH CinePass");
            response.put("description", "Movie Ticket Booking");
            response.put("reservationExpiresAt",
                    booking.getReservationExpiresAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            // Release seats if order creation or attachment fails.
            if (booking != null) {
                try {
                    bookingService.cancelPendingBooking(booking.getId());
                } catch (Exception ignored) {
                    // Keep the original error for the response.
                }
            }

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            safeMessage(e)));
        }
    }

    // STEP 2: Verify payment and confirm the booking.
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(
            @RequestBody Map<String, String> paymentData,
            Authentication authentication) {

        try {
            User loggedInUser = getLoggedInUser(authentication);

            String orderId = paymentData.get("razorpay_order_id");
            String paymentId = paymentData.get("razorpay_payment_id");
            String signature = paymentData.get("razorpay_signature");

            if (orderId == null || paymentId == null || signature == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Payment details are incomplete"));
            }

            // Confirm that this order belongs to the logged-in user.
            Booking booking = bookingService.getBookingByOrderId(orderId);

            if (!booking.getUserId().equals(
                    String.valueOf(loggedInUser.getId()))) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You cannot verify another user's booking"));
            }

            // Verify the signature against the stored order ID.
            if (!orderId.equals(booking.getRazorpayOrderId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid Razorpay order"));
            }

            razorpayService.verifyPaymentSignature(
                    booking.getRazorpayOrderId(),
                    paymentId,
                    signature);

            // Check Razorpay's actual payment status.
            razorpayService.verifyPaymentCaptured(paymentId);

            // Confirm only after both checks succeed.
            Booking confirmedBooking = bookingService.confirmPaidBooking(
                    booking.getRazorpayOrderId(),
                    paymentId);

            Map<String, Object> response = new HashMap<>();

            response.put("message", "Payment successful. Booking confirmed.");
            response.put("bookingId", confirmedBooking.getId());
            response.put("status", confirmedBooking.getStatus());
            response.put("paymentStatus", confirmedBooking.getPaymentStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", safeMessage(e)));
        }
    }

    // Cancel a pending booking when checkout is dismissed or payment fails.
    @PostMapping("/cancel/{bookingId}")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        try {
            User loggedInUser = getLoggedInUser(authentication);

            Booking booking = bookingService.getBookingById(bookingId);

            if (!booking.getUserId().equals(
                    String.valueOf(loggedInUser.getId()))) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You cannot cancel another user's booking"));
            }

            bookingService.cancelPendingBooking(bookingId);

            return ResponseEntity.ok(
                    Map.of("message", "Pending booking cancelled"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", safeMessage(e)));
        }
    }

    // Old direct booking is disabled. Payment is required.
    @PostMapping
    public ResponseEntity<?> createBooking() {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of(
                        "message",
                        "Direct booking is disabled. Use the payment checkout flow."));
    }

    // Get one booking: owner or admin only.
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            User user = getLoggedInUser(authentication);
            Booking booking = bookingService.getBookingById(id);

            if (!isAdmin(authentication) &&
                    !booking.getUserId().equals(String.valueOf(user.getId()))) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Users can only retrieve their own bookings.
    @GetMapping("/me")
    public ResponseEntity<List<Booking>> getMyBookings(
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        return ResponseEntity.ok(
                bookingService.getBookingsByUser(String.valueOf(user.getId())));
    }

    // Legacy user lookup: owner or admin only.
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(
            @PathVariable String userId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        String loggedInUserId = String.valueOf(user.getId());

        if (!isAdmin(authentication) && !loggedInUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    // All bookings are restricted to admins.
    @GetMapping
    public ResponseEntity<?> getAllBookings(Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // Show-level booking information is restricted to admins.
    @GetMapping("/show/{showId}")
    public ResponseEntity<?> getBookingsByShow(
            @PathVariable Long showId,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(bookingService.getBookingsByShow(showId));
    }

    private User getLoggedInUser(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof User)) {

            throw new IllegalStateException("Please log in to continue");
        }

        return (User) authentication.getPrincipal();
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String safeMessage(Exception e) {

        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return "The request could not be completed";
        }

        return e.getMessage();
    }
}
