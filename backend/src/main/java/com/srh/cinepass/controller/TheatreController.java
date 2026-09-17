package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.TheatreService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
@CrossOrigin(origins = "http://localhost:5173")
public class TheatreController {

    private final TheatreService theatreService;

    public TheatreController(TheatreService theatreService) {
        this.theatreService = theatreService;
    }

    @GetMapping
    public List<Theatre> getAllTheatres() {
        return theatreService.getAllTheatres();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Theatre> getTheatreById(
            @PathVariable Long id) {

        return theatreService
                .getTheatreById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/location/{locationId}")
    public List<Theatre> getTheatresByLocation(
            @PathVariable Long locationId) {

        return theatreService.getTheatresByLocation(locationId);
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

            List<Theatre> theatres =
                    theatreService.getTheatresByOwner(
                            user.getId()
                    );

            return ResponseEntity.ok(theatres);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addTheatre(
            @RequestBody Theatre theatre,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            return ResponseEntity.ok(
                    theatreService.addTheatre(
                            theatre,
                            user
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTheatre(
            @PathVariable Long id,
            @RequestBody Theatre theatreDetails,
            Authentication authentication) {

        try {

            User user =
                    (User) authentication.getPrincipal();

            return ResponseEntity.ok(
                    theatreService.updateTheatre(
                            id,
                            theatreDetails,
                            user
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
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

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}