package com.srh.cinepass;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.srh.cinepass.dto.TicketVerificationResponse;
import com.srh.cinepass.dto.VerificationResult;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.TicketStatus;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.exception.TicketOperationException;
import com.srh.cinepass.repository.TicketRepository;
import com.srh.cinepass.service.QrCodeService;
import com.srh.cinepass.service.SecureTicketTokenService;
import com.srh.cinepass.service.TicketVerificationService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketIssuanceTests extends TicketTestSupport {

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private SecureTicketTokenService tokenService;

    @Autowired
    private TicketVerificationService verificationService;

    @Autowired
    private TicketRepository tickets;

    @Test
    void issuesExactlyOneTicketForPaidConfirmationAndRetriesAreIdempotent() {
        User customer = createUser("Paid Customer", "USER");
        Movie movie = createMovie("Interstellar");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Booking booking = createBooking(customer, show, "PENDING", null);
        booking.setRazorpayOrderId("order_paid_1");
        bookingRepository.saveAndFlush(booking);
        reserveSeatsForPayment(booking);

        Booking confirmed = ticketServiceIssuance(booking.getRazorpayOrderId());

        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
        assertThat(tickets.findByBookingId(booking.getId())).isPresent();
        assertThat(tickets.count()).isEqualTo(1);

        // A retried payment callback must not create a second ticket.
        ticketServiceIssuance(booking.getRazorpayOrderId());

        assertThat(tickets.count()).isEqualTo(1);
    }

    private Booking ticketServiceIssuance(String orderId) {
        return bookingService().confirmPaidBooking(orderId, "pay_test_1");
    }

    private com.srh.cinepass.service.BookingService bookingService() {
        return applicationContextBookingService;
    }

    @Autowired
    private com.srh.cinepass.service.BookingService applicationContextBookingService;

    @Test
    void doesNotIssueTicketForUnconfirmedBooking() {
        User customer = createUser("Pending Customer", "USER");
        Movie movie = createMovie("Dune");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Booking booking = createBooking(customer, show, "PENDING", null);

        assertThatThrownBy(() -> issueTicket(booking))
                .isInstanceOf(TicketOperationException.class);

        assertThat(tickets.findByBookingId(booking.getId())).isEmpty();
    }

    @Test
    void generatesUniqueOpaqueTokensAndReferences() {
        User customer = createUser("Many Tickets", "USER");
        Movie movie = createMovie("Oppenheimer");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());

        Set<String> tokens = new HashSet<>();
        Set<String> references = new HashSet<>();

        for (int index = 0; index < 25; index++) {
            Booking booking = createConfirmedBooking(customer, show);
            Ticket ticket = issueTicket(booking);

            assertThat(ticket.getVerificationToken()).hasSize(43);
            assertThat(tokenService.isWellFormedToken(ticket.getVerificationToken()))
                    .isTrue();
            assertThat(ticket.getVerificationToken())
                    .isNotEqualTo(String.valueOf(booking.getId()));

            tokens.add(ticket.getVerificationToken());
            references.add(ticket.getTicketReference());
        }

        assertThat(tokens).hasSize(25);
        assertThat(references).hasSize(25);
    }

    @Test
    void qrPngContainsOnlyTheOpaqueToken() throws Exception {
        User customer = createUser("QR Customer", "USER");
        Movie movie = createMovie("The Matrix");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        byte[] png = qrCodeService.generatePng(ticket.getVerificationToken());

        assertThat(png).isNotEmpty();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));

        String decoded = new MultiFormatReader().decode(bitmap).getText();

        assertThat(decoded).isEqualTo(ticket.getVerificationToken());
        assertThat(decoded).doesNotContain(customer.getEmail());
        assertThat(decoded).doesNotContain(String.valueOf(customer.getId()));
    }

    @Test
    void cancellingBookingInvalidatesUnusedTicketButKeepsHistory() {
        User customer = createUser("Cancel Customer", "USER");
        User staff = createUser("Door Staff", "STAFF");
        Movie movie = createMovie("Tenet");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Booking booking = createConfirmedBooking(customer, show);
        Ticket ticket = issueTicket(booking);

        ticketService.cancelTicketsForBooking(booking.getId());

        Ticket reloaded = tickets.findById(ticket.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(reloaded.getCancelledAt()).isNotNull();

        TicketVerificationResponse response = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(response.result()).isEqualTo(VerificationResult.CANCELLED.name());
        assertThat(response.message()).isEqualTo("Booking Cancelled");

        // History is retained, not deleted.
        assertThat(tickets.findById(ticket.getId())).isPresent();
    }

    @Test
    void expiredTicketIsRejectedAndMarkedExpired() {
        User customer = createUser("Late Customer", "USER");
        User staff = createUser("Late Staff", "STAFF");
        Movie movie = createMovie("Inception");
        Show show = createShow(movie, showTimeExpired());
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        TicketVerificationResponse response = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(response.result()).isEqualTo(VerificationResult.EXPIRED.name());
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.EXPIRED);
    }

    @Test
    void ticketForFutureShowIsNotYetValid() {
        User customer = createUser("Early Customer", "USER");
        User staff = createUser("Early Staff", "STAFF");
        Movie movie = createMovie("Arrival");
        Show show = createShow(movie, showTimeNotYetValid());
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        TicketVerificationResponse response = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(response.result()).isEqualTo(VerificationResult.NOT_YET_VALID.name());
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.BOOKED);
    }

    @Test
    void invalidTokenDoesNotConsumeAValidTicket() {
        User customer = createUser("Owner Customer", "USER");
        User staff = createUser("Owner Staff", "STAFF");
        Movie movie = createMovie("Sicario");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        TicketVerificationResponse invalid = verificationService.verify(
                "x".repeat(43),
                staff);

        assertThat(invalid.result()).isEqualTo(VerificationResult.INVALID.name());
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.BOOKED);

        TicketVerificationResponse valid = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(valid.result()).isEqualTo(VerificationResult.VALID.name());
    }

    @Test
    void unconfirmedBookingIsRejected() {
        User customer = createUser("Unconfirmed Customer", "USER");
        User staff = createUser("Unconfirmed Staff", "STAFF");
        Movie movie = createMovie("Prisoners");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Booking booking = createConfirmedBooking(customer, show);
        Ticket ticket = issueTicket(booking);

        // Simulate a booking whose payment was never captured.
        booking.setStatus("PENDING");
        booking.setPaymentStatus(null);
        bookingRepository.saveAndFlush(booking);

        TicketVerificationResponse response = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(response.result()).isEqualTo(VerificationResult.NOT_CONFIRMED.name());
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.BOOKED);
    }

    @Test
    void ticketCannotBeAdmittedTwiceSequentially() {
        User customer = createUser("Double Customer", "USER");
        User staff = createUser("Double Staff", "STAFF");
        Movie movie = createMovie("Heat");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Ticket ticket = issueTicket(createConfirmedBooking(customer, show));

        TicketVerificationResponse first = verificationService.verify(
                ticket.getVerificationToken(),
                staff);
        TicketVerificationResponse second = verificationService.verify(
                ticket.getVerificationToken(),
                staff);

        assertThat(first.result()).isEqualTo(VerificationResult.VALID.name());
        assertThat(second.result()).isEqualTo(VerificationResult.ALREADY_USED.name());
        assertThat(second.message()).isEqualTo("Ticket Already Used");
    }

    @Test
    void effectiveStatusReflectsCancelledBookingWithoutPersistingAChange() {
        User customer = createUser("Status Customer", "USER");
        Movie movie = createMovie("Whiplash");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Booking booking = createConfirmedBooking(customer, show);
        Ticket ticket = issueTicket(booking);

        booking.setStatus("CANCELLED");
        bookingRepository.saveAndFlush(booking);

        assertThat(ticketService.toResponse(ticket).status())
                .isEqualTo(TicketStatus.CANCELLED.name());

        // The projection must not mutate the stored ticket.
        assertThat(tickets.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.BOOKED);
    }

    @Test
    void admissionWindowIsEnforcedUsingTheShowTime() {
        User customer = createUser("Window Customer", "USER");
        Movie movie = createMovie("Gravity");
        Show show = createShow(movie, LocalDateTime.now(testClock).minusHours(1));
        Booking booking = createConfirmedBooking(customer, show);
        Ticket ticket = issueTicket(booking);

        assertThat(ticketService.showStartTime(booking))
                .isEqualTo(show.getShowTime() == null ? null
                        : LocalDateTime.of(show.getShowDate(), show.getShowTime()));
    }
}
