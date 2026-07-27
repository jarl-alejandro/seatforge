package com.jarl.seatforge.inventory.application.port.in;

import java.time.Duration;

/** Internal use case used by the inventory expiration job. */
public interface ReleaseExpiredReservations {
    ReleaseResult releaseBatch(int batchSize);

    record ReleaseResult(Duration duration, int candidates, int released, int conflicts) {
        public ReleaseResult {
            if (duration.isNegative() || candidates < 0 || released < 0 || conflicts < 0
                    || released + conflicts != candidates) {
                throw new IllegalArgumentException("invalid expiration release metrics");
            }
        }
    }
}
