package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.inventory.application.port.in.ReleaseExpiredReservations;
import com.jarl.seatforge.inventory.application.port.out.ExpiredReservationStore;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class ReleaseExpiredReservationsHandler implements ReleaseExpiredReservations {
    private final ExpiredReservationStore store;
    private final Clock clock;
    private final LongSupplier nanoTime;

    public ReleaseExpiredReservationsHandler(ExpiredReservationStore store, Clock clock) {
        this(store, clock, System::nanoTime);
    }

    ReleaseExpiredReservationsHandler(ExpiredReservationStore store, Clock clock, LongSupplier nanoTime) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
        this.nanoTime = Objects.requireNonNull(nanoTime);
    }

    @Override
    public ReleaseResult releaseBatch(int batchSize) {
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be greater than zero");
        long startedAt = nanoTime.getAsLong();
        ExpiredReservationStore.BatchOutcome outcome = store.releaseExpired(clock.instant(), batchSize);
        Duration duration = Duration.ofNanos(Math.max(0, nanoTime.getAsLong() - startedAt));
        return new ReleaseResult(duration, outcome.candidates(), outcome.released(), outcome.conflicts());
    }
}
