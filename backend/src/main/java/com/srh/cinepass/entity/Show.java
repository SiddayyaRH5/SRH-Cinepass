package com.srh.cinepass.entity;

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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Theatre getTheatre() { return theatre; }
    public void setTheatre(Theatre theatre) { this.theatre = theatre; }

    public LocalDateTime getShowTime() { return showTime; }
    public void setShowTime(LocalDateTime showTime) { this.showTime = showTime; }
    public void setShowTime(LocalDate date) { 
        this.showTime = date != null ? LocalDateTime.of(date, this.showTime != null ? this.showTime.toLocalTime() : LocalTime.MIDNIGHT) : null; 
    }
    public void setShowTime(LocalTime time) { 
        this.showTime = time != null ? LocalDateTime.of(this.showTime != null ? this.showTime.toLocalDate() : LocalDate.now(), time) : null; 
    }

    public LocalDate getShowDate() { 
        return showTime != null ? showTime.toLocalDate() : null; 
    }
    public void setShowDate(LocalDate date) { 
        this.showTime = date != null ? LocalDateTime.of(date, this.showTime != null ? this.showTime.toLocalTime() : LocalTime.MIDNIGHT) : null; 
    }

    public Double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(Double ticketPrice) { this.ticketPrice = ticketPrice; }
}