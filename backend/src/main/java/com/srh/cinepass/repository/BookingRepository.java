
package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByUserEmail(String userEmail);

    List<Booking> findByShowId(Long showId);

    // Find a booking using its Razorpay order ID.
    Optional<Booking> findByRazorpayOrderId(String razorpayOrderId);

    // Find pending bookings whose temporary reservations have expired.
    List<Booking> findByStatusAndReservationExpiresAtBefore(
            String status,
            LocalDateTime expiryTime);
}
