package com.jarl.seatforge.identity.infrastructure;

import com.jarl.seatforge.identity.infrastructure.security.IdentitySecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(IdentitySecurityConfiguration.class)
public class IdentityModuleConfiguration {
}
