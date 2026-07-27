package com.jarl.seatforge.inventory.application.port.out;

import java.time.Instant;

/** Atomic persistence boundary for expiring a bounded set of reservations. */
public interface ExpiredReservationStore {
    BatchOutcome releaseExpired(Instant expiresOnOrBefore, int batchSize);

    record BatchOutcome(int candidates, int released, int conflicts) {
        public BatchOutcome {
            if (candidates < 0 || released < 0 || conflicts < 0
                    || released + conflicts != candidates) {
                throw new IllegalArgumentException("invalid expiration batch outcome");
            }
        }
    }
}
