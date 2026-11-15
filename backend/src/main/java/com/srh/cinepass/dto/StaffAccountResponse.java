package com.srh.cinepass.dto;

public record StaffAccountResponse(
        Long id,
        String name,
        String email,
        String role
) {
}
