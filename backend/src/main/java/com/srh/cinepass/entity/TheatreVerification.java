package com.srh.cinepass.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "theatre_verifications")
public class TheatreVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String theatreName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false, length = 15)
    private String gstin;

    @Column(nullable = false, length = 10)
    private String panNumber;

    @Column(nullable = false)
    private String registrationNumber;

    @Column(nullable = false)
    private String mobileNumber;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false)
    private Integer numberOfScreens;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private String theatreType;

    @Column(nullable = false)
    private Long locationId;

    private String gstDocumentUrl;
    private String businessDocumentUrl;
    private String theatrePhotoUrl;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(length = 1000)
    private String rejectionReason;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    public TheatreVerification() {}

    @PrePersist
    public void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public Integer getNumberOfScreens() { return numberOfScreens; }
    public void setNumberOfScreens(Integer numberOfScreens) { this.numberOfScreens = numberOfScreens; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public String getTheatreType() { return theatreType; }
    public void setTheatreType(String theatreType) { this.theatreType = theatreType; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public String getGstDocumentUrl() { return gstDocumentUrl; }
    public void setGstDocumentUrl(String gstDocumentUrl) { this.gstDocumentUrl = gstDocumentUrl; }

    public String getBusinessDocumentUrl() { return businessDocumentUrl; }
    public void setBusinessDocumentUrl(String businessDocumentUrl) { this.businessDocumentUrl = businessDocumentUrl; }

    public String getTheatrePhotoUrl() { return theatrePhotoUrl; }
    public void setTheatrePhotoUrl(String theatrePhotoUrl) { this.theatrePhotoUrl = theatrePhotoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }

    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}