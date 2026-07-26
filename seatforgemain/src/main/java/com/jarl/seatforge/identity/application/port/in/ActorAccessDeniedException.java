package com.jarl.seatforge.identity.application.port.in;

public final class ActorAccessDeniedException extends RuntimeException {

    public ActorAccessDeniedException() {
        super("The authenticated actor does not own the requested resource");
    }
}
