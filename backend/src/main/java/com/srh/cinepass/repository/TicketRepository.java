package com.srh.cinepass.repository;

import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    boolean existsByVerificationToken(String verificationToken);

    boolean existsByTicketReference(String ticketReference);

    /**
     * The row lock is what makes verification atomic. Two staff members who
     * scan the same QR at the same time are serialized here; the second one
     * observes USED after the first transaction commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.verificationToken = :token")
    Optional<Ticket> findByVerificationTokenForUpdate(@Param("token") String token);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.booking b
            JOIN FETCH b.show s
            JOIN FETCH s.movie
            JOIN FETCH s.theatre th
            LEFT JOIN FETCH th.location
            WHERE t.id = :ticketId
            """)
    Optional<Ticket> findDetailedById(@Param("ticketId") Long ticketId);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.booking b
            JOIN FETCH b.show s
            JOIN FETCH s.movie
            JOIN FETCH s.theatre th
            LEFT JOIN FETCH th.location
            WHERE b.userId = :userId
            ORDER BY b.bookingDate DESC
            """)
    List<Ticket> findAllDetailedForUser(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Ticket t
            SET t.status = :cancelled,
                t.cancelledAt = :now
            WHERE t.booking.id = :bookingId
              AND t.status = :booked
            """)
    int cancelUnusedForBooking(
            @Param("bookingId") Long bookingId,
            @Param("cancelled") TicketStatus cancelled,
            @Param("booked") TicketStatus booked,
            @Param("now") LocalDateTime now);
}
