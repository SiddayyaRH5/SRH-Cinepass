package com.srh.cinepass;

import com.srh.cinepass.dto.TicketVerificationResponse;
import com.srh.cinepass.entity.Booking;
import com.srh.cinepass.entity.Movie;
import com.srh.cinepass.entity.Show;
import com.srh.cinepass.entity.Ticket;
import com.srh.cinepass.entity.User;
import com.srh.cinepass.service.TicketVerificationService;
import com.srh.cinepass.support.TicketTestSupport;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TicketConcurrencyTests extends TicketTestSupport {

    @Autowired
    private TicketVerificationService verificationService;

    @Test
    void concurrentScansOfTheSameTicketAdmitExactlyOneStaffMember() throws Exception {
        User customer = createUser("Race Customer", "USER");
        User firstStaff = createUser("First Gate", "STAFF");
        User secondStaff = createUser("Second Gate", "STAFF");
        Movie movie = createMovie("Mad Max");
        Show show = createShow(movie, showTimeWithinAdmissionWindow());
        Booking booking = createConfirmedBooking(customer, show);
        Ticket ticket = issueTicket(booking);

        String token = ticket.getVerificationToken();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<String> first = scan(start, token, firstStaff);
            Callable<String> second = scan(start, token, secondStaff);

            Future<String> firstResult = executor.submit(first);
            Future<String> secondResult = executor.submit(second);

            start.countDown();

            List<String> results = new ArrayList<>();
            results.add(firstResult.get(30, TimeUnit.SECONDS));
            results.add(secondResult.get(30, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder("VALID", "ALREADY_USED");

            Ticket reloaded = ticketRepository.findById(ticket.getId()).orElseThrow();

            assertThat(reloaded.getStatus().name()).isEqualTo("USED");
            assertThat(reloaded.getVerifiedAt()).isNotNull();
            assertThat(reloaded.getVerifiedBy()).isNotNull();
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<String> scan(CountDownLatch start, String token, User staff) {
        return () -> {
            start.await(10, TimeUnit.SECONDS);

            try {
                TicketVerificationResponse response = verificationService.verify(token, staff);
                return response.result();
            } catch (org.springframework.dao.PessimisticLockingFailureException exception) {
                // A lock timeout must never be converted into admission. Treat
                // it as "not admitted" for the purpose of this assertion.
                return "LOCK_TIMEOUT";
            }
        };
    }
}
