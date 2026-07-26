package com.jarl.seatforge.identity.infrastructure;

import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.identity.infrastructure.security.IdentitySecurityConfiguration;
import com.jarl.seatforge.identity.infrastructure.security.SpringSecurityCurrentActor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(IdentitySecurityConfiguration.class)
public class IdentityModuleConfiguration {

    @Bean
    CurrentActor currentActor() {
        return new SpringSecurityCurrentActor();
    }
}
