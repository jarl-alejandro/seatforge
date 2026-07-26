package com.jarl.seatforge.events.application.port.in;

public final class EventPublicationConflictException extends RuntimeException {
    public EventPublicationConflictException(String message) {
        super(message);
    }
}
