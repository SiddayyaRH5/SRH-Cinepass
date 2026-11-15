package com.srh.cinepass.seed;

import com.srh.cinepass.service.TicketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Confirmed bookings created before this feature existed still deserve a
 * ticket. The query only selects bookings with no ticket, and the unique
 * booking constraint makes this safe to run on every startup.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.tickets",
        name = "backfill-on-startup",
        havingValue = "true",
        matchIfMissing = true)
public class TicketBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TicketBackfillRunner.class);

    private final TicketService ticketService;

    public TicketBackfillRunner(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int issued = ticketService.backfillMissingTickets();

            if (issued > 0) {
                log.info("Issued {} ticket(s) for existing confirmed bookings", issued);
            }

        } catch (Exception exception) {
            /*
             * Do not prevent the application from starting because of an
             * individual backfill failure. No booking status is changed here;
             * the operation is retried on the next startup.
             */
            log.error("Ticket backfill could not be completed; it will be retried on next startup", exception);
        }
    }
}
