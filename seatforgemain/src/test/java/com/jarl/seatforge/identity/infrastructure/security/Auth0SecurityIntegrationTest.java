package com.jarl.seatforge.identity.infrastructure.security;

import com.jarl.seatforge.identity.application.port.in.AuthenticatedActor;
import com.jarl.seatforge.identity.application.port.in.ActorId;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import com.jarl.seatforge.identity.infrastructure.IdentityModuleConfiguration;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = Auth0SecurityIntegrationTest.SecurityProbeController.class, properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://seatforge-test.auth0.com/",
        "spring.security.oauth2.resourceserver.jwt.audiences=https://api.seatforge.local"
})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({
        IdentityModuleConfiguration.class,
        Auth0SecurityIntegrationTest.SecurityProbeController.class,
        Auth0SecurityIntegrationTest.OfflineJwtConfiguration.class
})
class Auth0SecurityIntegrationTest {

    private static final String ISSUER = "https://seatforge-test.auth0.com/";
    private static final String AUDIENCE = "https://api.seatforge.local";
    private static final String ORGANIZER_SUBJECT = "organizer-client@clients";
    private static final String BUYER_SUBJECT = "buyer-client@clients";

    private static KeyPair signingKeys;
    private static JwtEncoder jwtEncoder;

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    static void createSigningKeys() throws Exception {
        signingKeys = generateKeyPair();
        jwtEncoder = encoder(signingKeys, "seatforge-test-key");
    }

    @Test
    void public_catalog_does_not_require_a_token() throws Exception {
        mvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void organizer_token_propagates_subject_and_role() throws Exception {
        String token = token(jwtEncoder, ISSUER, AUDIENCE, ORGANIZER_SUBJECT,
                Set.of("create:events", "publish:events"), Instant.now().plusSeconds(300));

        mvc.perform(post("/api/v1/events").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorId").value(ORGANIZER_SUBJECT))
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    void buyer_token_can_reserve_and_propagates_subject_and_role() throws Exception {
        String token = token(jwtEncoder, ISSUER, AUDIENCE, BUYER_SUBJECT,
                Set.of("reserve:tickets"), Instant.now().plusSeconds(300));

        mvc.perform(post("/api/v1/tickets/{ticketId}/reservations", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorId").value(BUYER_SUBJECT))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void missing_token_returns_problem_401() throws Exception {
        mvc.perform(post("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void insufficient_permission_returns_problem_403_without_leaking_token() throws Exception {
        String token = token(jwtEncoder, ISSUER, AUDIENCE, BUYER_SUBJECT,
                Set.of("reserve:tickets"), Instant.now().plusSeconds(300));

        String response = mvc.perform(post("/api/v1/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(token);
    }

    @Test
    void malformed_expired_wrong_issuer_and_wrong_audience_tokens_return_401() throws Exception {
        assertUnauthorized("not-a-jwt");
        assertUnauthorized(token(jwtEncoder, ISSUER, AUDIENCE, BUYER_SUBJECT,
                Set.of("reserve:tickets"), Instant.now().minusSeconds(60)));
        assertUnauthorized(token(jwtEncoder, "https://another-issuer.example/", AUDIENCE, BUYER_SUBJECT,
                Set.of("reserve:tickets"), Instant.now().plusSeconds(300)));
        assertUnauthorized(token(jwtEncoder, ISSUER, "https://another-api.example", BUYER_SUBJECT,
                Set.of("reserve:tickets"), Instant.now().plusSeconds(300)));
    }

    @Test
    void token_signed_by_an_unknown_key_returns_401() throws Exception {
        JwtEncoder unknownEncoder = encoder(generateKeyPair(), "unknown-key");
        String token = token(unknownEncoder, ISSUER, AUDIENCE, BUYER_SUBJECT,
                Set.of("reserve:tickets"), Instant.now().plusSeconds(300));

        assertUnauthorized(token);
    }

    @Test
    void token_using_an_algorithm_other_than_rs256_returns_401() throws Exception {
        String token = token(
                jwtEncoder,
                ISSUER,
                AUDIENCE,
                BUYER_SUBJECT,
                Set.of("reserve:tickets"),
                Instant.now().plusSeconds(300),
                SignatureAlgorithm.RS512
        );

        assertUnauthorized(token);
    }

    @Test
    void actor_with_buyer_and_organizer_permissions_is_rejected_as_ambiguous() throws Exception {
        String token = token(jwtEncoder, ISSUER, AUDIENCE, ORGANIZER_SUBJECT,
                Set.of("create:events", "reserve:tickets"), Instant.now().plusSeconds(300));

        mvc.perform(post("/api/v1/events").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void organizer_cannot_operate_on_a_resource_owned_by_another_actor() throws Exception {
        String token = token(jwtEncoder, ISSUER, AUDIENCE, ORGANIZER_SUBJECT,
                Set.of("publish:events"), Instant.now().plusSeconds(300));

        mvc.perform(post("/api/v1/events/{eventId}/publish", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Test-Owner-Id", "another-organizer@clients"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private void assertUnauthorized(String token) throws Exception {
        String response = mvc.perform(post("/api/v1/tickets/{ticketId}/reservations", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(token);
    }

    private static String token(
            JwtEncoder encoder,
            String issuer,
            String audience,
            String subject,
            Set<String> permissions,
            Instant expiresAt
    ) {
        return token(encoder, issuer, audience, subject, permissions, expiresAt, SignatureAlgorithm.RS256);
    }

    private static String token(
            JwtEncoder encoder,
            String issuer,
            String audience,
            String subject,
            Set<String> permissions,
            Instant expiresAt,
            SignatureAlgorithm algorithm
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .audience(List.of(audience))
                .issuedAt(expiresAt.minusSeconds(300))
                .expiresAt(expiresAt)
                .claim("permissions", permissions)
                .build();
        JwsHeader header = JwsHeader.with(algorithm).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static JwtEncoder encoder(KeyPair keys, String keyId) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
                .privateKey((RSAPrivateKey) keys.getPrivate())
                .keyID(keyId)
                .build();
        return new NimbusJwtEncoder((selector, context) -> selector.select(new JWKSet(rsaKey)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OfflineJwtConfiguration {

        @Bean
        @Primary
        JwtDecoder offlineJwtDecoder() {
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withPublicKey((RSAPublicKey) signingKeys.getPublic())
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();

            OAuth2TokenValidator<Jwt> issuerAndTime = JwtValidators.createDefaultWithIssuer(ISSUER);
            OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                    "aud",
                    audiences -> audiences != null && audiences.contains(AUDIENCE)
            );
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndTime, audience));
            return decoder;
        }
    }

    @RestController
    @RequestMapping("/api/v1")
    static class SecurityProbeController {

        private final CurrentActor currentActor;

        SecurityProbeController(CurrentActor currentActor) {
            this.currentActor = currentActor;
        }

        @GetMapping("/events")
        String publicCatalog() {
            return "public";
        }

        @PostMapping("/events")
        Map<String, String> createEvent() {
            return actorResponse();
        }

        @PostMapping("/tickets/{ticketId}/reservations")
        Map<String, String> reserveTicket(@PathVariable UUID ticketId) {
            return actorResponse();
        }

        @PostMapping("/events/{eventId}/publish")
        Map<String, String> publishEvent(
                @PathVariable UUID eventId,
                @RequestHeader("X-Test-Owner-Id") String ownerId
        ) {
            AuthenticatedActor actor = currentActor.get();
            actor.requireOwnership(new ActorId(ownerId));
            return actorResponse();
        }

        private Map<String, String> actorResponse() {
            AuthenticatedActor actor = currentActor.get();
            return Map.of("actorId", actor.id().value(), "role", actor.role().name());
        }
    }
}
