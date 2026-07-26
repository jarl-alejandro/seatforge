package com.jarl.seatforge.shared.infrastructure;

import com.jarl.seatforge.shared.application.port.in.PublishDomainEventUseCase;
import com.jarl.seatforge.shared.application.port.out.DomainEventPublisher;
import com.jarl.seatforge.shared.application.usecase.PublishDomainEventHandler;
import com.jarl.seatforge.shared.infrastructure.messaging.SpringDomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SharedModuleConfiguration {

    @Bean
    DomainEventPublisher domainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringDomainEventPublisher(applicationEventPublisher);
    }

    @Bean
    PublishDomainEventUseCase publishDomainEventUseCase(DomainEventPublisher domainEventPublisher) {
        return new PublishDomainEventHandler(domainEventPublisher);
    }
}
