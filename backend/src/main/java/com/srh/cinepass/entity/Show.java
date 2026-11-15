package com.srh.cinepass.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srh.cinepass.util.BusinessTime;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theatre theatre;

    @Column(name = "show_time", nullable = false)
    private LocalDateTime showTime;

    @Column(name = "ticket_price", nullable = false)
    private Double ticketPrice;

    /*
     * Nullable on purpose: shows created before this feature have no value and
     * are treated as SCHEDULED. Cleanup only ever moves SCHEDULED/null rows to
     * EXPIRED; it never deletes shows, bookings, tickets or payments.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ShowStatus status;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Theatre getTheatre() { return theatre; }
    public void setTheatre(Theatre theatre) { this.theatre = theatre; }

    // Return LocalTime to match ShowService expectations
    public LocalTime getShowTime() { 
        return showTime != null ? showTime.toLocalTime() : null; 
    }
    public void setShowTime(LocalTime time) { 
        this.showTime = time != null ? LocalDateTime.of(this.showTime != null ? this.showTime.toLocalDate() : BusinessTime.today(), time) : null;
    }
    public void setShowTime(LocalDateTime dateTime) { 
        this.showTime = dateTime; 
    }

    public LocalDate getShowDate() { 
        return showTime != null ? showTime.toLocalDate() : null; 
    }
    public void setShowDate(LocalDate date) { 
        this.showTime = date != null ? LocalDateTime.of(date, this.showTime != null ? this.showTime.toLocalTime() : LocalTime.MIDNIGHT) : null; 
    }

    public Double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(Double ticketPrice) { this.ticketPrice = ticketPrice; }

    /**
     * Full IST wall-clock start instant. Kept out of JSON so the existing
     * {@code showDate}/{@code showTime} contract stays unchanged for clients.
     */
    @JsonIgnore
    public LocalDateTime getShowDateTime() { return showTime; }

    public ShowStatus getStatus() { return status; }
    public void setStatus(ShowStatus status) { this.status = status; }
}