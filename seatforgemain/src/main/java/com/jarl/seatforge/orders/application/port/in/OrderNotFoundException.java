package com.jarl.seatforge.orders.application.port.in;

public final class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException() {
        super("The order does not exist or is not visible to the authenticated buyer");
    }
}
