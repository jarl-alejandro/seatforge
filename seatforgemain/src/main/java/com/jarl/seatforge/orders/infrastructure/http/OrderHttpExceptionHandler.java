package com.jarl.seatforge.orders.infrastructure.http;

import com.jarl.seatforge.contract.model.Problem;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderAccessDeniedException;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderConflictException;
import com.jarl.seatforge.inventory.application.port.in.ReservationForOrderNotFoundException;
import com.jarl.seatforge.orders.application.port.in.OrderConflictException;
import com.jarl.seatforge.orders.application.port.in.OrderIdempotencyConflictException;
import com.jarl.seatforge.orders.application.port.in.OrderNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = OrdersHttpAdapter.class)
public final class OrderHttpExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class})
    ResponseEntity<Problem> invalid(Exception exception) {
        String detail = exception instanceof IllegalArgumentException
                ? exception.getMessage() : "The request does not satisfy the order contract";
        return problem(400, "Invalid order", detail, "INVALID_ORDER");
    }

    @ExceptionHandler({OrderNotFoundException.class, ReservationForOrderNotFoundException.class})
    ResponseEntity<Problem> notFound(RuntimeException exception) {
        return problem(404, "Order resource not found", exception.getMessage(), "ORDER_RESOURCE_NOT_FOUND");
    }

    @ExceptionHandler(ReservationForOrderAccessDeniedException.class)
    ResponseEntity<Problem> forbidden(ReservationForOrderAccessDeniedException exception) {
        return problem(403, "Reservation access denied", exception.getMessage(), "RESERVATION_ACCESS_DENIED");
    }

    @ExceptionHandler({OrderConflictException.class, OrderIdempotencyConflictException.class,
            ReservationForOrderConflictException.class})
    ResponseEntity<Problem> conflict(RuntimeException exception) {
        String code = exception instanceof OrderIdempotencyConflictException
                ? "IDEMPOTENCY_CONFLICT" : "ORDER_CONFLICT";
        return problem(409, "Order conflict", exception.getMessage(), code);
    }

    private static ResponseEntity<Problem> problem(int status, String title, String detail, String code) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new Problem("about:blank", title, status).detail(detail).code(code));
    }
}
