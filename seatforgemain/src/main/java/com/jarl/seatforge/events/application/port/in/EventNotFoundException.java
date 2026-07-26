package com.jarl.seatforge.events.application.port.in;

public final class EventNotFoundException extends RuntimeException {
    public EventNotFoundException() {
        super("event does not exist or is not publicly visible");
    }
}
