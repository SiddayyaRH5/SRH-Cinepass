package com.srh.cinepass.service;

import com.srh.cinepass.dto.StaffAccountRequest;
import com.srh.cinepass.dto.StaffAccountResponse;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketOperationException;
import com.srh.cinepass.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Staff accounts can only be created or promoted by an authenticated ADMIN.
 * There is no public staff signup path.
 */
@Service
public class StaffAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffAdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StaffAccountResponse> listStaff() {
        return userRepository.findByRoleOrderByNameAsc("STAFF")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StaffAccountResponse createOrPromote(StaffAccountRequest request) {
        if (request == null) {
            throw new TicketOperationException("Staff account details are required");
        }

        String email = request.email() == null
                ? ""
                : request.email().trim().toLowerCase(Locale.ROOT);

        if (email.isBlank()) {
            throw new TicketOperationException("Email is required");
        }

        User existing = userRepository.findByEmail(email).orElse(null);

        if (existing != null) {
            if ("ADMIN".equalsIgnoreCase(existing.getRole())) {
                throw new TicketOperationException(
                        "Administrators already have scanner access");
            }

            if (!"USER".equalsIgnoreCase(existing.getRole())
                    && !"STAFF".equalsIgnoreCase(existing.getRole())) {
                throw new TicketOperationException(
                        "Only customer accounts can be promoted to staff");
            }

            if (request.name() != null && !request.name().isBlank()) {
                existing.setName(request.name().trim());
            }

            if (request.password() != null && !request.password().isBlank()) {
                existing.setPassword(passwordEncoder.encode(request.password()));
            }

            existing.setRole("STAFF");
            return toResponse(userRepository.save(existing));
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new TicketOperationException("Name is required for a new staff account");
        }

        if (request.password() == null || request.password().length() < 8) {
            throw new TicketOperationException(
                    "A password of at least 8 characters is required for a new staff account");
        }

        User staff = new User();
        staff.setName(request.name().trim());
        staff.setEmail(email);
        staff.setPassword(passwordEncoder.encode(request.password()));
        staff.setRole("STAFF");

        return toResponse(userRepository.save(staff));
    }

    private StaffAccountResponse toResponse(User user) {
        return new StaffAccountResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }
}
