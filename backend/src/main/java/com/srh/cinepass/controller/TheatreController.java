package com.srh.cinepass.controller;

import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.service.TheatreService;
import org.springframework.http.ResponseEntity;
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

        return theatreService.getTheatreById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Theatre> addTheatre(
            @RequestBody Theatre theatre) {

        Theatre savedTheatre =
                theatreService.addTheatre(theatre);

        return ResponseEntity.ok(savedTheatre);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Theatre> updateTheatre(
            @PathVariable Long id,
            @RequestBody Theatre theatreDetails) {

        try {
            Theatre updatedTheatre =
                    theatreService.updateTheatre(id, theatreDetails);

            return ResponseEntity.ok(updatedTheatre);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheatre(
            @PathVariable Long id) {

        try {
            theatreService.deleteTheatre(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
@GetMapping("/location/{locationId}")
public List<Theatre> getTheatresByLocation(
        @PathVariable Long locationId) {

    return theatreService.getTheatresByLocation(locationId);
}
}