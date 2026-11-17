package com.srh.cinepass.controller;

import com.srh.cinepass.dto.TheatreResponse;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketAccessDeniedException;
import com.srh.cinepass.service.TheatreService;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
public class TheatreController {

    private final TheatreService theatreService;

    public TheatreController(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    @GetMapping
    @Cacheable(value = "theatres", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<TheatreResponse> getAllTheatres() {
        return theatreService.getAllTheatres()
                .stream()
                .map(TheatreResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Cacheable(value = "theatres", key = "'id:' + #id", unless = "#result == null")
    public ResponseEntity<TheatreResponse> getTheatreById(
            @PathVariable Long id) {

        return theatreService
                .getTheatreById(id)
                .map(TheatreResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/location/{locationId}")
    @Cacheable(value = "theatres", key = "'loc:' + #locationId", unless = "#result == null || #result.isEmpty()")
    public List<TheatreResponse> getTheatresByLocation(
            @PathVariable Long locationId) {

        return theatreService.getTheatresByLocation(locationId)
                .stream()
                .map(TheatreResponse::from)
                .toList();
    }

    /*
     * THEATRE OWNER - MY THEATRES
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyTheatres(
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            List<TheatreResponse> theatres =
                    theatreService.getTheatresByOwner(
                            user.getId()
                    ).stream()
                    .map(TheatreResponse::from)
                    .toList();

            return ResponseEntity.ok(theatres);

        } catch (TicketAccessDeniedException e) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping
    @CacheEvict(value = "theatres", allEntries = true)
    public ResponseEntity<?> addTheatre(
            @RequestBody Theatre theatre,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            return ResponseEntity.ok(
                    TheatreResponse.from(
                            theatreService.addTheatre(
                                    theatre,
                                    user
                            )
                    )
            );

        } catch (TicketAccessDeniedException e) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @CacheEvict(value = "theatres", allEntries = true)
    public ResponseEntity<?> updateTheatre(
            @PathVariable Long id,
            @RequestBody Theatre theatreDetails,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            return ResponseEntity.ok(
                    TheatreResponse.from(
                            theatreService.updateTheatre(
                                    id,
                                    theatreDetails,
                                    user
                            )
                    )
            );

        } catch (TicketAccessDeniedException e) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "theatres", allEntries = true)
    public ResponseEntity<?> deleteTheatre(
            @PathVariable Long id,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            theatreService.deleteTheatre(
                    id,
                    user
            );

            return ResponseEntity
                    .noContent()
                    .build();

        } catch (TicketAccessDeniedException e) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}
