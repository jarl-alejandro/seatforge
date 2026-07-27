package com.jarl.seatforge.inventory.application.usecase;

import com.jarl.seatforge.inventory.application.port.out.ExpiredReservationStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReleaseExpiredReservationsHandlerTest {
    private static final Instant NOW = Instant.parse("2026-07-26T15:00:00Z");

    @Test
    void uses_injected_clock_and_reports_batch_metrics_and_duration() {
        CapturingStore store = new CapturingStore(new ExpiredReservationStore.BatchOutcome(5, 3, 2));
        AtomicLong nanoTime = new AtomicLong();
        ReleaseExpiredReservationsHandler handler = new ReleaseExpiredReservationsHandler(
                store, Clock.fixed(NOW, ZoneOffset.UTC), () -> nanoTime.getAndAdd(4_000_000));

        var result = handler.releaseBatch(5);

        assertThat(store.cutoff).isEqualTo(NOW);
        assertThat(store.batchSize).isEqualTo(5);
        assertThat(result.duration().toMillis()).isEqualTo(4);
        assertThat(result.candidates()).isEqualTo(5);
        assertThat(result.released()).isEqualTo(3);
        assertThat(result.conflicts()).isEqualTo(2);
    }

    @Test
    void retry_is_delegated_with_the_same_cutoff_and_has_no_double_effect() {
        AtomicInteger calls = new AtomicInteger();
        ExpiredReservationStore store = (cutoff, batch) -> calls.getAndIncrement() == 0
                ? new ExpiredReservationStore.BatchOutcome(1, 1, 0)
                : new ExpiredReservationStore.BatchOutcome(0, 0, 0);
        var handler = new ReleaseExpiredReservationsHandler(store, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(handler.releaseBatch(10).released()).isEqualTo(1);
        assertThat(handler.releaseBatch(10).released()).isZero();
    }

    @Test
    void rejects_non_positive_batches_without_touching_the_store() {
        CapturingStore store = new CapturingStore(new ExpiredReservationStore.BatchOutcome(0, 0, 0));
        var handler = new ReleaseExpiredReservationsHandler(store, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> handler.releaseBatch(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.cutoff).isNull();
    }

    private static final class CapturingStore implements ExpiredReservationStore {
        private final BatchOutcome outcome;
        private Instant cutoff;
        private int batchSize;

        private CapturingStore(BatchOutcome outcome) { this.outcome = outcome; }

        @Override
        public BatchOutcome releaseExpired(Instant cutoff, int batchSize) {
            this.cutoff = cutoff;
            this.batchSize = batchSize;
            return outcome;
        }
    }
}
