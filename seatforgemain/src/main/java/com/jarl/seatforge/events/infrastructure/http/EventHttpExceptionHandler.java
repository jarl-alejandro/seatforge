package com.jarl.seatforge.events.infrastructure.http;

import com.jarl.seatforge.contract.model.Problem;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EventsHttpAdapter.class)
public class EventHttpExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
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
}
