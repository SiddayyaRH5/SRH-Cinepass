
package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Booking;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(String userId);

    List<Booking> findByUserEmail(String userEmail);

    List<Booking> findByShowId(Long showId);

    /*
     * Used to decide whether a show may be physically deleted. Any booking
     * (pending, confirmed or cancelled) keeps the show as history.
     */
    boolean existsByShowId(Long showId);

    // Find a booking using its Razorpay order ID.
    Optional<Booking> findByRazorpayOrderId(String razorpayOrderId);

    /*
     * Payment confirmation callbacks can be retried. Locking the booking row
     * makes confirmation (and the one-ticket-per-booking insert) serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.razorpayOrderId = :orderId")
    Optional<Booking> findByRazorpayOrderIdForUpdate(
            @Param("orderId") String orderId);

    /*
     * Cancellation and order attachment must serialize with webhook/browser
     * confirmation on the same booking row. Without this lock a stale
     * cancellation can overwrite a captured payment with CANCELLED/FAILED.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    // Find pending bookings whose temporary reservations have expired.
    List<Booking> findByStatusAndReservationExpiresAtBefore(
            String status,
            LocalDateTime expiryTime);

    // Captured payments that could not be confirmed automatically.
    List<Booking> findByPaymentRecoveryStatusOrderByBookingDateDesc(
            String paymentRecoveryStatus);

    /*
     * ------------------------------------------------------------
     * Movie lifecycle support
     * ------------------------------------------------------------
     *
     * A movie may only be retired when nothing about its bookings is still
     * open. These counts let the lifecycle job decide that without loading
     * any booking into memory.
     */

    /**
     * Bookings of a movie that still have to be fulfilled: pending payment,
     * confirmed, or parked for manual payment review.
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.show.movie.id = :movieId
              AND b.status IN :statuses
            """)
    long countActiveBookingsForMovie(
            @Param("movieId") Long movieId,
            @Param("statuses") List<String> statuses);

    /**
     * Bookings of a movie whose payment workflow is still unresolved, for
     * example a captured payment that could not be confirmed automatically.
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.show.movie.id = :movieId
              AND b.paymentRecoveryStatus = :recoveryStatus
            """)
    long countUnresolvedPaymentWorkflowsForMovie(
            @Param("movieId") Long movieId,
            @Param("recoveryStatus") String recoveryStatus);

    /*
     * Existing confirmed bookings (created before this feature) need a ticket
     * too. Backfill is idempotent and only selects bookings without a ticket.
     */
    @Query("""
            SELECT b.id
            FROM Booking b
            WHERE b.status = 'CONFIRMED'
              AND b.paymentStatus = 'PAID'
              AND NOT EXISTS (
                    SELECT t.id FROM Ticket t WHERE t.booking = b
              )
            ORDER BY b.id ASC
            """)
    List<Long> findConfirmedBookingIdsWithoutTicket();
}
