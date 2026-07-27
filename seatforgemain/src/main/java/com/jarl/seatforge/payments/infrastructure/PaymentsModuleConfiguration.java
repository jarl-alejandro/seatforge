package com.jarl.seatforge.payments.infrastructure;

import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.payments.application.port.in.SimulatePaymentUseCase;
import com.jarl.seatforge.payments.application.port.out.PaymentStore;
import com.jarl.seatforge.payments.application.usecase.SimulatePaymentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class PaymentsModuleConfiguration {
    @Bean SimulatePaymentUseCase simulatePaymentUseCase(CurrentActor actor, PaymentStore store,
            Clock seatForgeClock, @Value("${seatforge.payments.max-delay-ms:30000}") int maxDelayMs) {
        return new SimulatePaymentHandler(actor,store,seatForgeClock,maxDelayMs);
    }
}
