package com.jarl.seatforge.payments.application.port.in;
public final class SimulatedPaymentTimeoutException extends RuntimeException { public SimulatedPaymentTimeoutException() { super("The simulated payment timed out without changing state"); } }
