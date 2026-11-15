package com.srh.cinepass.exception;

public class TicketAccessDeniedException extends RuntimeException {
    public TicketAccessDeniedException() {
        super("You do not have access to this ticket");
    }

    public TicketAccessDeniedException(String message) {
        super(message);
    }
}
