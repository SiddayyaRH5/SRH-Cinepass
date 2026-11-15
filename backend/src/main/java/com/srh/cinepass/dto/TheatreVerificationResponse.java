package com.srh.cinepass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.srh.cinepass.entity.TheatreVerification;
import com.srh.cinepass.entity.User;

import java.time.LocalDateTime;

/**
 * Safe projection of a theatre-verification application.
 *
 * <p>The entity is deliberately never serialized directly: it contains a
 * {@link User} association whose password hash must never reach a client.
 * The applicant's name and email are included only for the administrator
 * review screen; no password, password hash or session data is exposed.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TheatreVerificationResponse(
        Long id,
        String theatreName,
        String ownerName,
        String gstin,
        String panNumber,
        String registrationNumber,
        String mobileNumber,
        String address,
        String city,
        String state,
        String pincode,
        Integer numberOfScreens,
        Integer totalSeats,
        String theatreType,
        Long locationId,
        String gstDocumentUrl,
        String businessDocumentUrl,
        String theatrePhotoUrl,
        String status,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String applicantName,
        String applicantEmail
) {

    public static TheatreVerificationResponse from(TheatreVerification verification) {
        if (verification == null) {
            return null;
        }

        User applicant = verification.getUser();

        return new TheatreVerificationResponse(
                verification.getId(),
                verification.getTheatreName(),
                verification.getOwnerName(),
                verification.getGstin(),
                verification.getPanNumber(),
                verification.getRegistrationNumber(),
                verification.getMobileNumber(),
                verification.getAddress(),
                verification.getCity(),
                verification.getState(),
                verification.getPincode(),
                verification.getNumberOfScreens(),
                verification.getTotalSeats(),
                verification.getTheatreType(),
                verification.getLocationId(),
                verification.getGstDocumentUrl(),
                verification.getBusinessDocumentUrl(),
                verification.getTheatrePhotoUrl(),
                verification.getStatus(),
                verification.getRejectionReason(),
                verification.getSubmittedAt(),
                verification.getReviewedAt(),
                applicant != null ? applicant.getName() : null,
                applicant != null ? applicant.getEmail() : null);
    }
}
