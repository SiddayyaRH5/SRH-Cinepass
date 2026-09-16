package com.srh.cinepass.service;

import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.repository.TheatreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TheatreService {

    private final TheatreRepository theatreRepository;

    public TheatreService(TheatreRepository theatreRepository) {
        this.theatreRepository = theatreRepository;
    }

    public List<Theatre> getAllTheatres() {
        return theatreRepository.findAll();
    }

    public Optional<Theatre> getTheatreById(Long id) {
        return theatreRepository.findById(id);
    }

    public Theatre addTheatre(Theatre theatre) {
        return theatreRepository.save(theatre);
    }

    public Theatre updateTheatre(
            Long id,
            Theatre theatreDetails) {

        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Theatre not found with id: " + id));

        theatre.setName(theatreDetails.getName());
        theatre.setTotalSeats(theatreDetails.getTotalSeats());
        theatre.setLocation(theatreDetails.getLocation());

        return theatreRepository.save(theatre);
    }

    public void deleteTheatre(Long id) {

        if (!theatreRepository.existsById(id)) {
            throw new RuntimeException(
                    "Theatre not found with id: " + id);
        }

        theatreRepository.deleteById(id);
    }
public List<Theatre> getTheatresByLocation(Long locationId) {
    return theatreRepository.findByLocationId(locationId);
}
}