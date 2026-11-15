package com.srh.cinepass.support;

import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Location;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.RazorpayWebhookEvent;
import com.srh.cinepass.entity.Seat;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Theatre;
import com.srh.cinepass.entity.TheatreVerification;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.repository.BookingRepository;
import com.srh.cinepass.repository.LocationRepository;
import com.srh.cinepass.repository.MovieRepository;
import com.srh.cinepass.repository.RazorpayWebhookEventRepository;
import com.srh.cinepass.repository.SeatRepository;
import com.srh.cinepass.repository.ShowRepository;
import com.srh.cinepass.repository.TheatreRepository;
import com.srh.cinepass.repository.TheatreVerificationRepository;
import com.srh.cinepass.repository.TicketRepository;
import com.srh.cinepass.repository.UserRepository;
import com.srh.cinepass.service.JwtService;
import com.srh.cinepass.service.TicketService;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared integration-test fixture. The full Spring context is used so that
 * security rules, JWT authentication, JPA locking and the real transaction
 * boundaries are exercised rather than mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
public abstract class TicketTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TicketService ticketService;

    @Autowired
    protected TicketRepository ticketRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected ShowRepository showRepository;

    @Autowired
    protected SeatRepository seatRepository;

    @Autowired
    protected TheatreRepository theatreRepository;

    @Autowired
    protected LocationRepository locationRepository;

    @Autowired
    protected MovieRepository movieRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TheatreVerificationRepository theatreVerificationRepository;

    @Autowired
    protected RazorpayWebhookEventRepository webhookEventRepository;

    @Autowired
    protected MutableClock testClock;

    @BeforeEach
    void resetDatabase() {
        testClock.setInstant(Instant.now());

        webhookEventRepository.deleteAll();
        theatreVerificationRepository.deleteAll();
        ticketRepository.deleteAll();
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        showRepository.deleteAll();
        theatreRepository.deleteAll();
        locationRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String name, String role) {
        String email = name.toLowerCase().replace(" ", ".") + "."
                + SEQUENCE.incrementAndGet() + "@example.test";

        User user = new User(
                name,
                email,
                passwordEncoder.encode("Password123!"),
                role);

        return userRepository.saveAndFlush(user);
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user.getEmail());
    }

    protected Movie createMovie(String title) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setPosterUrl("https://example.test/" + SEQUENCE.incrementAndGet() + ".jpg");
        movie.setDuration(120);
        return movieRepository.saveAndFlush(movie);
    }

    protected Location createLocation(String city) {
        Location location = new Location(
                city + " " + SEQUENCE.incrementAndGet(),
                "Test State",
                "India");

        return locationRepository.saveAndFlush(location);
    }

    protected Theatre createTheatre(User owner, String name) {
        Theatre theatre = new Theatre(name, 100, createLocation("Theatre City"));
        theatre.setOwner(owner);
        return theatreRepository.saveAndFlush(theatre);
    }

    protected Show createShow(Movie movie, LocalDateTime showDateTime) {
        Location location = new Location(
                "Test City " + SEQUENCE.incrementAndGet(),
                "Test State",
                "India");

        locationRepository.saveAndFlush(location);

        Theatre theatre = new Theatre(
                "Test Theatre " + SEQUENCE.incrementAndGet(),
                100,
                location);

        theatreRepository.saveAndFlush(theatre);

        Show show = new Show();
        show.setMovie(movie);
        show.setTheatre(theatre);
        show.setShowTime(showDateTime);
        show.setTicketPrice(250.0);

        return showRepository.saveAndFlush(show);
    }

    protected Booking createBooking(
            User customer,
            Show show,
            String status,
            String paymentStatus) {

        Booking booking = new Booking();
        booking.setUserId(String.valueOf(customer.getId()));
        booking.setUserEmail(customer.getEmail());
        booking.setShow(show);
        booking.setSeatNumbers(new ArrayList<>(List.of("S1", "S2")));
        booking.setNumberOfSeats(2);
        booking.setTotalAmount(500.0);
        booking.setStatus(status);
        booking.setPaymentStatus(paymentStatus);
        booking.setBookingDate(LocalDateTime.now(testClock).minusMinutes(5));
        booking.setReservationExpiresAt(null);

        return bookingRepository.saveAndFlush(booking);
    }

    protected Booking createConfirmedBooking(User customer, Show show) {
        return createBooking(customer, show, "CONFIRMED", "PAID");
    }

    protected Ticket issueTicket(Booking booking) {
        return ticketService.issueForConfirmedBooking(booking);
    }

    /**
     * Recreates the temporary seat reservation that the real payment flow
     * leaves behind so the paid-confirmation path can be exercised end to end.
     */
    protected Booking reserveSeatsForPayment(Booking booking) {
        LocalDateTime expiresAt = LocalDateTime.now(testClock).plusMinutes(10);

        booking.setReservationExpiresAt(expiresAt);
        Booking saved = bookingRepository.saveAndFlush(booking);

        for (String seatNumber : saved.getSeatNumbers()) {
            Seat seat = new Seat();
            seat.setSeatNumber(seatNumber);
            seat.setShow(saved.getShow());
            seat.setReservedByBookingId(saved.getId());
            seat.setReservedUntil(expiresAt);
            seatRepository.saveAndFlush(seat);
        }

        return saved;
    }

    protected TheatreVerification createTheatreVerification(User owner, String status) {
        Location location = new Location(
                "Verification City " + SEQUENCE.incrementAndGet(),
                "Test State",
                "India");

        locationRepository.saveAndFlush(location);

        TheatreVerification verification = new TheatreVerification();
        verification.setUser(owner);
        verification.setTheatreName("Verification Theatre " + SEQUENCE.incrementAndGet());
        verification.setOwnerName(owner.getName());
        verification.setGstin("29ABCDE1234F1Z5");
        verification.setPanNumber("ABCDE1234F");
        verification.setRegistrationNumber("REG-12345");
        verification.setMobileNumber("9876543210");
        verification.setAddress("1 Test Road");
        verification.setCity("Test City");
        verification.setState("Test State");
        verification.setPincode("560001");
        verification.setNumberOfScreens(3);
        verification.setTotalSeats(300);
        verification.setTheatreType("MULTIPLEX");
        verification.setLocationId(location.getId());
        verification.setStatus(status);

        return theatreVerificationRepository.saveAndFlush(verification);
    }

    protected LocalDateTime showTimeWithinAdmissionWindow() {
        return LocalDateTime.now(testClock).plusMinutes(30).withSecond(0).withNano(0);
    }

    protected LocalDateTime showTimeNotYetValid() {
        return LocalDateTime.now(testClock).plusHours(5).withSecond(0).withNano(0);
    }

    protected LocalDateTime showTimeExpired() {
        return LocalDateTime.now(testClock).minusHours(3).withSecond(0).withNano(0);
    }
}
