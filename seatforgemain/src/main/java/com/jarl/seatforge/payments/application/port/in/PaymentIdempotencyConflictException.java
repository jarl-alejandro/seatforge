package com.jarl.seatforge.payments.application.port.in;
public final class PaymentIdempotencyConflictException extends RuntimeException { public PaymentIdempotencyConflictException() { super("Idempotency-Key was already used for another payment request"); } }
