package com.jarl.seatforge.inventory.infrastructure.expiration;

import com.jarl.seatforge.inventory.application.port.in.ReleaseExpiredReservations;
import com.jarl.seatforge.inventory.application.port.out.ExpiredReservationStore;
import com.jarl.seatforge.inventory.application.usecase.ReleaseExpiredReservationsHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "seatforge.inventory.expiration", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ExpiredReservationJobConfiguration {
    @Bean
    ReleaseExpiredReservations releaseExpiredReservations(ExpiredReservationStore store, Clock seatForgeClock) {
        return new ReleaseExpiredReservationsHandler(store, seatForgeClock);
    }

    @Bean
    ExpiredReservationJob expiredReservationJob(
            ReleaseExpiredReservations useCase,
            @Value("${seatforge.inventory.expiration.batch-size:100}") int batchSize) {
        return new ExpiredReservationJob(useCase, batchSize);
    }
}
