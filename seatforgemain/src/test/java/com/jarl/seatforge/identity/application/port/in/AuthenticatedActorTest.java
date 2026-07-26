package com.jarl.seatforge.identity.application.port.in;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedActorTest {

    @Test
    void exposes_provider_neutral_identity_permissions_and_ownership() {
        ActorId actorId = new ActorId("organizer-client@clients");
        AuthenticatedActor actor = new AuthenticatedActor(
                actorId,
                ActorRole.ORGANIZER,
                Set.of("create:events", "publish:events")
        );

        assertThat(actor.id()).isEqualTo(actorId);
        assertThat(actor.role()).isEqualTo(ActorRole.ORGANIZER);
        assertThat(actor.hasPermission("publish:events")).isTrue();
        assertThat(actor.owns(new ActorId("organizer-client@clients"))).isTrue();
        assertThat(actor.owns(new ActorId("another-organizer@clients"))).isFalse();
        assertThatThrownBy(() -> actor.requireOwnership(new ActorId("another-organizer@clients")))
                .isInstanceOf(ActorAccessDeniedException.class);
    }
}
