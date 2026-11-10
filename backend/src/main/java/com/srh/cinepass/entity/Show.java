package com.srh.cinepass.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // Compatible with ShowService expecting LocalDate
    public LocalDate getShowTime() { 
        return showTime != null ? showTime.toLocalDate() : null; 
    }
    public void setShowTime(LocalDateTime showTime) { 
        this.showTime = showTime; 
    }
    public void setShowTime(LocalDate showDate) { 
        this.showTime = showDate != null ? showDate.atStartOfDay() : null; 
    }

    public LocalDate getShowDate() { 
        return showTime != null ? showTime.toLocalDate() : null; 
    }
    public void setShowDate(LocalDate showDate) { 
        this.showTime = showDate != null ? showDate.atStartOfDay() : null; 
    }

    public Double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(Double ticketPrice) { this.ticketPrice = ticketPrice; }
}