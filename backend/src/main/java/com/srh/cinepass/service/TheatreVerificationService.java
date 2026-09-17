package com.srh.cinepass.service;

import com.srh.cinepass.dto.TheatreVerificationRequest;
import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.TheatreVerification;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.LocationRepository;
import com.srh.cinepass.repository.TheatreRepository;
import com.srh.cinepass.repository.TheatreVerificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TheatreVerificationService {

    private final TheatreVerificationRepository verificationRepository;
    private final LocationRepository locationRepository;
    private final TheatreRepository theatreRepository;

    public TheatreVerificationService(
            TheatreVerificationRepository verificationRepository,
            LocationRepository locationRepository,
            TheatreRepository theatreRepository) {

        this.verificationRepository = verificationRepository;
        this.locationRepository = locationRepository;
        this.theatreRepository = theatreRepository;
    }

    // =========================================================
    // SUBMIT THEATRE VERIFICATION
    // =========================================================

    @Transactional
    public TheatreVerification submit(
            TheatreVerificationRequest request,
            User user) {

        if (!"THEATRE_OWNER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException(
                    "Only theatre owners can submit theatre verification");
        }

        validate(request);

        TheatreVerification verification =
                verificationRepository.findByUserId(user.getId())
                        .orElse(new TheatreVerification());

        // Already approved owners cannot submit another application
        if ("APPROVED".equalsIgnoreCase(verification.getStatus())) {
            throw new RuntimeException(
                    "Your theatre has already been approved");
        }

        Location location =
                locationRepository.findById(request.getLocationId())
                        .orElseThrow(() ->
                                new RuntimeException("Invalid location"));

        verification.setUser(user);

        verification.setTheatreName(
                request.getTheatreName().trim());

        verification.setOwnerName(
                request.getOwnerName().trim());

        verification.setGstin(
                request.getGstin().trim().toUpperCase());

        verification.setPanNumber(
                request.getPanNumber().trim().toUpperCase());

        verification.setRegistrationNumber(
                request.getRegistrationNumber().trim());

        verification.setMobileNumber(
                request.getMobileNumber().trim());

        verification.setAddress(
                request.getAddress().trim());

        verification.setCity(
                request.getCity().trim());

        verification.setState(
                request.getState().trim());

        verification.setPincode(
                request.getPincode().trim());

        verification.setNumberOfScreens(
                request.getNumberOfScreens());

        verification.setTotalSeats(
                request.getTotalSeats());

        verification.setTheatreType(
                request.getTheatreType().trim());

        verification.setLocationId(
                location.getId());

        verification.setGstDocumentUrl(
                request.getGstDocumentUrl());

        verification.setBusinessDocumentUrl(
                request.getBusinessDocumentUrl());

        verification.setTheatrePhotoUrl(
                request.getTheatrePhotoUrl());

        // Every new submission becomes PENDING
        verification.setStatus("PENDING");

        // Clear previous rejection
        verification.setRejectionReason(null);

        return verificationRepository.save(verification);
    }


    // =========================================================
    // GET MY VERIFICATION
    // =========================================================

    public TheatreVerification getMyVerification(User user) {

        return verificationRepository
                .findByUserId(user.getId())
                .orElse(null);
    }


    // =========================================================
    // GET PENDING APPLICATIONS
    // ADMIN ONLY
    // =========================================================

    public List<TheatreVerification> getPendingApplications() {

        return verificationRepository
                .findByStatusOrderBySubmittedAtAsc("PENDING");
    }


    // =========================================================
    // CHECK WHETHER OWNER IS APPROVED
    // =========================================================

    public boolean isOwnerApproved(Long userId) {

        return verificationRepository
                .existsByUserIdAndStatus(
                        userId,
                        "APPROVED");
    }


    // =========================================================
    // GET APPROVED VERIFICATION FOR OWNER
    // =========================================================

    public TheatreVerification getApprovedVerification(Long userId) {

        return verificationRepository
                .findByUserId(userId)
                .filter(v ->
                        "APPROVED".equalsIgnoreCase(v.getStatus()))
                .orElse(null);
    }


    // =========================================================
    // APPROVE APPLICATION
    // ADMIN ONLY
    // =========================================================

    @Transactional
    public TheatreVerification approve(Long id) {

        TheatreVerification verification =
                verificationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Verification application not found"));

        if ("APPROVED".equalsIgnoreCase(
                verification.getStatus())) {

            throw new RuntimeException(
                    "Application is already approved");
        }

        Location location =
                locationRepository
                        .findById(verification.getLocationId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Location not found"));

        // Prevent duplicate theatre
        if (theatreRepository.existsByNameIgnoreCaseAndLocationId(
                verification.getTheatreName(),
                location.getId())) {

            throw new RuntimeException(
                    "A theatre with this name already exists in this city");
        }

        // Create actual theatre
        Theatre theatre = new Theatre();

        theatre.setName(
                verification.getTheatreName());

        theatre.setTotalSeats(
                verification.getTotalSeats());

        theatre.setLocation(
                location);

        theatre.setOwner(
                verification.getUser());

        theatreRepository.save(theatre);

        // Update verification
        verification.setStatus("APPROVED");

        verification.setReviewedAt(
                LocalDateTime.now());

        verification.setRejectionReason(null);

        return verificationRepository.save(
                verification);
    }


    // =========================================================
    // REJECT APPLICATION
    // ADMIN ONLY
    // =========================================================

    @Transactional
    public TheatreVerification reject(
            Long id,
            String reason) {

        TheatreVerification verification =
                verificationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Verification application not found"));

        if (reason == null ||
                reason.trim().isEmpty()) {

            throw new RuntimeException(
                    "Rejection reason is required");
        }

        verification.setStatus("REJECTED");

        verification.setRejectionReason(
                reason.trim());

        verification.setReviewedAt(
                LocalDateTime.now());

        return verificationRepository.save(
                verification);
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validate(
            TheatreVerificationRequest request) {

        if (blank(request.getTheatreName())) {
            throw new RuntimeException(
                    "Theatre name is required");
        }

        if (blank(request.getOwnerName())) {
            throw new RuntimeException(
                    "Owner name is required");
        }

        if (blank(request.getGstin())) {
            throw new RuntimeException(
                    "GSTIN is required");
        }

        if (!request.getGstin()
                .trim()
                .toUpperCase()
                .matches(
                        "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")) {

            throw new RuntimeException(
                    "Invalid GSTIN format");
        }

        if (blank(request.getPanNumber())) {
            throw new RuntimeException(
                    "PAN number is required");
        }

        if (!request.getPanNumber()
                .trim()
                .toUpperCase()
                .matches(
                        "[A-Z]{5}[0-9]{4}[A-Z]")) {

            throw new RuntimeException(
                    "Invalid PAN format");
        }

        if (blank(request.getRegistrationNumber())) {
            throw new RuntimeException(
                    "Business registration number is required");
        }

        if (blank(request.getMobileNumber())) {
            throw new RuntimeException(
                    "Mobile number is required");
        }

        if (!request.getMobileNumber()
                .trim()
                .matches(
                        "[6-9][0-9]{9}")) {

            throw new RuntimeException(
                    "Invalid mobile number");
        }

        if (blank(request.getAddress())) {
            throw new RuntimeException(
                    "Theatre address is required");
        }

        if (blank(request.getCity())) {
            throw new RuntimeException(
                    "City is required");
        }

        if (blank(request.getState())) {
            throw new RuntimeException(
                    "State is required");
        }

        if (blank(request.getPincode()) ||
                !request.getPincode()
                        .trim()
                        .matches("[0-9]{6}")) {

            throw new RuntimeException(
                    "Invalid pincode");
        }

        if (request.getNumberOfScreens() == null ||
                request.getNumberOfScreens() < 1 ||
                request.getNumberOfScreens() > 100) {

            throw new RuntimeException(
                    "Number of screens must be between 1 and 100");
        }

        if (request.getTotalSeats() == null ||
                request.getTotalSeats() < 10 ||
                request.getTotalSeats() > 20000) {

            throw new RuntimeException(
                    "Total seats must be between 10 and 20000");
        }

        if (blank(request.getTheatreType())) {
            throw new RuntimeException(
                    "Theatre type is required");
        }

        if (request.getLocationId() == null) {
            throw new RuntimeException(
                    "Location is required");
        }
    }


    // =========================================================
    // EMPTY CHECK
    // =========================================================

    private boolean blank(String value) {

        return value == null ||
                value.trim().isEmpty();
    }
}