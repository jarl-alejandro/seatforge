package com.jarl.seatforge.payments.application.port.in;
public final class PaymentNotFoundException extends RuntimeException { public PaymentNotFoundException() { super("Order not found"); } }
