package com.jarl.seatforge.identity.application.port.in;

import java.util.Objects;

public record ActorId(String value) {

    public ActorId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
