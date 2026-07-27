package com.jarl.seatforge.inventory.infrastructure.expiration;

import com.jarl.seatforge.inventory.application.port.in.ReleaseExpiredReservations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

final class ExpiredReservationJob {
    private static final Logger log = LoggerFactory.getLogger(ExpiredReservationJob.class);
    private final ReleaseExpiredReservations useCase;
    private final int batchSize;

    ExpiredReservationJob(ReleaseExpiredReservations useCase, int batchSize) {
        this.useCase = useCase;
        if (batchSize < 1) throw new IllegalArgumentException("expiration batch size must be greater than zero");
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${seatforge.inventory.expiration.fixed-delay:PT30S}",
            initialDelayString = "${seatforge.inventory.expiration.initial-delay:PT30S}")
    void releaseExpiredReservations() {
        ReleaseExpiredReservations.ReleaseResult result = useCase.releaseBatch(batchSize);
        log.info("Expired reservation sweep: durationMs={}, candidates={}, released={}, conflicts={}",
                result.duration().toMillis(), result.candidates(), result.released(), result.conflicts());
    }
}
