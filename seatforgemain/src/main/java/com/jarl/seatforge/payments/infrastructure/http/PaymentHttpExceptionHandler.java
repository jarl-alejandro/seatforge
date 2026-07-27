package com.jarl.seatforge.payments.infrastructure.http;

import com.jarl.seatforge.contract.model.Problem;
import com.jarl.seatforge.payments.application.port.in.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=PaymentsHttpAdapter.class)
public final class PaymentHttpExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,ConstraintViolationException.class})
    ResponseEntity<Problem> invalid(Exception e){return problem(400,"Invalid payment",e.getMessage(),"INVALID_PAYMENT");}
    @ExceptionHandler(PaymentNotFoundException.class) ResponseEntity<Problem> missing(RuntimeException e){return problem(404,"Order not found",e.getMessage(),"ORDER_NOT_FOUND");}
    @ExceptionHandler({PaymentConflictException.class,PaymentIdempotencyConflictException.class}) ResponseEntity<Problem> conflict(RuntimeException e){return problem(409,"Payment conflict",e.getMessage(),e instanceof PaymentIdempotencyConflictException?"IDEMPOTENCY_CONFLICT":"PAYMENT_CONFLICT");}
    @ExceptionHandler(SimulatedPaymentTimeoutException.class) ResponseEntity<Problem> timeout(RuntimeException e){return problem(504,"Simulated payment timeout",e.getMessage(),"PAYMENT_TIMEOUT");}
    private static ResponseEntity<Problem> problem(int status,String title,String detail,String code){return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(new Problem("about:blank",title,status).detail(detail).code(code));}
}
