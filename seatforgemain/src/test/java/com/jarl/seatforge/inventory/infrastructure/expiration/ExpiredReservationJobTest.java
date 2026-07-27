package com.jarl.seatforge.inventory.infrastructure.expiration;

import com.jarl.seatforge.inventory.application.port.in.ReleaseExpiredReservations;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpiredReservationJobTest {
    @Test
    void runs_one_configured_batch_per_tick() {
        AtomicInteger receivedBatch = new AtomicInteger();
        ReleaseExpiredReservations useCase = batch -> {
            receivedBatch.set(batch);
            return new ReleaseExpiredReservations.ReleaseResult(Duration.ofMillis(2), 3, 2, 1);
        };

        new ExpiredReservationJob(useCase, 37).releaseExpiredReservations();

        assertThat(receivedBatch).hasValue(37);
    }

    @Test
    void rejects_invalid_configuration() {
        ReleaseExpiredReservations useCase = batch ->
                new ReleaseExpiredReservations.ReleaseResult(Duration.ZERO, 0, 0, 0);
        assertThatThrownBy(() -> new ExpiredReservationJob(useCase, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
