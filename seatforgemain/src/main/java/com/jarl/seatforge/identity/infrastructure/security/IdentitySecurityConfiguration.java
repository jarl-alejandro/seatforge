package com.jarl.seatforge.identity.infrastructure.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IdentitySecurityConfiguration {

    @Bean
    SecurityFilterChain seatForgeSecurityFilterChain(HttpSecurity http) throws Exception {
        Auth0JwtAuthenticationConverter jwtConverter = new Auth0JwtAuthenticationConverter();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/tickets").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/events")
                            .hasAuthority(SeatForgePermissions.CREATE_EVENTS)
                        .requestMatchers(HttpMethod.POST, "/api/v1/events/*/publish")
                            .hasAuthority(SeatForgePermissions.PUBLISH_EVENTS)
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets/*/reservations")
                            .hasAuthority(SeatForgePermissions.RESERVE_TICKETS)
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders")
                            .hasAuthority(SeatForgePermissions.CREATE_ORDERS)
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*")
                            .hasAuthority(SeatForgePermissions.READ_ORDERS)
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/payments")
                            .hasAuthority(SeatForgePermissions.PAY_ORDERS)
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityProblemWriter.unauthorized(response))
                        .accessDeniedHandler((request, response, exception) ->
                                SecurityProblemWriter.forbidden(response))
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityProblemWriter.unauthorized(response))
                        .accessDeniedHandler((request, response, exception) ->
                                SecurityProblemWriter.forbidden(response))
                );

        return http.build();
    }

}
