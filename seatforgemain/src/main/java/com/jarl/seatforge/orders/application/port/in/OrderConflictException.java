package com.jarl.seatforge.orders.application.port.in;

public final class OrderConflictException extends RuntimeException {
    public OrderConflictException() {
        super("The reservation already has an order");
    }
}
