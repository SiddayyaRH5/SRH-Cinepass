package com.srh.cinepass.service;

import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.TheatreRepository;
import com.srh.cinepass.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service("ownerAuthorization")
public class OwnerAuthorizationService {

    private final TheatreRepository theatreRepository;
    private final UserRepository userRepository;
    private final TheatreVerificationService verificationService;

    public OwnerAuthorizationService(
            TheatreRepository theatreRepository,
            UserRepository userRepository,
            TheatreVerificationService verificationService) {

        this.theatreRepository = theatreRepository;
        this.userRepository = userRepository;
        this.verificationService = verificationService;
    }

    public boolean isApprovedOwner(Long userId) {

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }

        if (!"THEATRE_OWNER".equalsIgnoreCase(user.getRole())) {
            return false;
        }

        return verificationService.isOwnerApproved(userId);
    }

    public boolean ownsTheatre(Long userId, Long theatreId) {

        if (!isApprovedOwner(userId)) {
            return false;
        }

        Theatre theatre = theatreRepository.findById(theatreId).orElse(null);

        if (theatre == null) {
            return false;
        }

        if (theatre.getOwner() == null) {
            return false;
        }

        return theatre.getOwner().getId().equals(userId);
    }

    public boolean canManageTheatre(Long userId, Long theatreId) {

        return ownsTheatre(userId, theatreId);
    }
}