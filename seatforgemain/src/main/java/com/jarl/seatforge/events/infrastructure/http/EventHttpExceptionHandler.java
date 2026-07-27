package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.contract.model.Problem;
import com.jarl.seatforge.events.application.port.in.EventNotFoundException;
import com.jarl.seatforge.events.application.port.in.EventPublicationConflictException;
import jakarta.validation.ConstraintViolationException;
import com.jarl.seatforge.inventory.application.port.in.ReservationIdempotencyConflictException;
import com.jarl.seatforge.inventory.application.port.in.TicketNotFoundException;
import com.jarl.seatforge.inventory.application.port.in.TicketReservationConflictException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {EventsHttpAdapter.class, EventTicketsHttpAdapter.class})
public class EventHttpExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class})
    ResponseEntity<Problem> invalidRequest(Exception exception) {
        String detail = exception instanceof IllegalArgumentException
                ? exception.getMessage()
                : "The request does not satisfy the event contract";
        Problem problem = new Problem("about:blank", "Invalid event", 400)
                .detail(detail)
                .code("INVALID_EVENT");
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(EventNotFoundException.class)
    ResponseEntity<Problem> eventNotFound(EventNotFoundException exception) {
        return problem(404, "Event not found", exception.getMessage(), "EVENT_NOT_FOUND");
    }

    @ExceptionHandler(EventPublicationConflictException.class)
    ResponseEntity<Problem> publicationConflict(EventPublicationConflictException exception) {
        return problem(409, "Event cannot be published", exception.getMessage(), "EVENT_PUBLICATION_CONFLICT");
    }

    @ExceptionHandler(TicketNotFoundException.class)
    ResponseEntity<Problem> ticketNotFound(TicketNotFoundException exception) {
        return problem(404, "Ticket not found", exception.getMessage(), "TICKET_NOT_FOUND");
    }

    @ExceptionHandler(TicketReservationConflictException.class)
    ResponseEntity<Problem> ticketReservationConflict(TicketReservationConflictException exception) {
        return problem(409, "Ticket cannot be reserved", exception.getMessage(),
                "TICKET_RESERVATION_CONFLICT");
    }

    @ExceptionHandler(ReservationIdempotencyConflictException.class)
    ResponseEntity<Problem> reservationIdempotencyConflict(
            ReservationIdempotencyConflictException exception) {
        return problem(409, "Idempotency conflict", exception.getMessage(),
                "IDEMPOTENCY_CONFLICT");
    }

    private static ResponseEntity<Problem> problem(int status, String title, String detail, String code) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new Problem("about:blank", title, status).detail(detail).code(code));
    }
}
