package com.jarl.seatforge.identity.infrastructure.security;

import com.jarl.seatforge.identity.application.port.in.ActorAccessDeniedException;
import com.jarl.seatforge.identity.application.port.in.ActorIdentityUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
final class IdentitySecurityExceptionHandler {

    @ExceptionHandler({ActorIdentityUnavailableException.class, ActorAccessDeniedException.class})
    ProblemDetail actorIdentityUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "The authenticated principal does not map to one SeatForge actor."
        );
        problem.setTitle("Forbidden");
        problem.setProperty("code", "FORBIDDEN");
        return problem;
    }
}
