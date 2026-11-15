package com.srh.cinepass.controller;

import com.srh.cinepass.dto.StaffAccountRequest;
import com.srh.cinepass.dto.StaffAccountResponse;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.service.StaffAdminService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    private final StaffAdminService staffAdminService;

    public AdminStaffController(StaffAdminService staffAdminService) {
        this.staffAdminService = staffAdminService;
    }

    @GetMapping
    public ResponseEntity<List<StaffAccountResponse>> list(Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(staffAdminService.listStaff());
    }

    @PostMapping
    public ResponseEntity<StaffAccountResponse> createOrPromote(
            @Valid @RequestBody StaffAccountRequest request,
            Authentication authentication) {

        requireAdmin(authentication);
        return ResponseEntity.ok(staffAdminService.createOrPromote(request));
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)
                || !"ADMIN".equalsIgnoreCase(user.getRole())) {

            throw new TicketAccessDeniedException();
        }
    }
}
