package com.srh.cinepass.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "theatres")
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer totalSeats;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /*
     * Theatre owner.
     *
     * Nullable so existing ADMIN-created theatres
     * continue working.
     */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Theatre() {
    }

    public Theatre(
            String name,
            Integer totalSeats,
            Location location) {

        this.name = name;
        this.totalSeats = totalSeats;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}