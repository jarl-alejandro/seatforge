package com.jarl.seatforge.identity.infrastructure.security;

import com.jarl.seatforge.identity.application.port.in.ActorId;
import com.jarl.seatforge.identity.application.port.in.ActorIdentityUnavailableException;
import com.jarl.seatforge.identity.application.port.in.ActorRole;
import com.jarl.seatforge.identity.application.port.in.AuthenticatedActor;
import com.jarl.seatforge.identity.application.port.in.CurrentActor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

final class SpringSecurityCurrentActor implements CurrentActor {

    @Override
    public AuthenticatedActor get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ActorIdentityUnavailableException();
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return new AuthenticatedActor(
                new ActorId(authentication.getName()),
                role(authorities),
                authorities.stream()
                        .filter(authority -> !authority.startsWith("ROLE_"))
                        .collect(Collectors.toUnmodifiableSet())
        );
    }

    private ActorRole role(Set<String> authorities) {
        boolean buyer = authorities.contains(SeatForgePermissions.ROLE_BUYER);
        boolean organizer = authorities.contains(SeatForgePermissions.ROLE_ORGANIZER);

        if (buyer == organizer) {
            throw new ActorIdentityUnavailableException();
        }
        return buyer ? ActorRole.BUYER : ActorRole.ORGANIZER;
    }
}
