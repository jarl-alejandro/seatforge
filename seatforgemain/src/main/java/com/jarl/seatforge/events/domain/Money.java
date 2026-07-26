package com.jarl.seatforge.events.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    public static final String USD = "USD";

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("price must have at most two decimal places");
        }
        if (!USD.equals(currency)) {
            throw new IllegalArgumentException("currency must be USD");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
