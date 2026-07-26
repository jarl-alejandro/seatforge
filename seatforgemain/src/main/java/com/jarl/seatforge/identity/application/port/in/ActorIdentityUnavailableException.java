package com.jarl.seatforge.identity.application.port.in;

public final class ActorIdentityUnavailableException extends RuntimeException {

    public ActorIdentityUnavailableException() {
        super("The authenticated principal does not map to one SeatForge actor");
    }
}
