package com.srh.cinepass.entity;

import jakarta.persistence.*;

@Entity
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer totalSeats;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    public Theatre() {
    }

    public Theatre(String name, Integer totalSeats, Location location) {
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
}