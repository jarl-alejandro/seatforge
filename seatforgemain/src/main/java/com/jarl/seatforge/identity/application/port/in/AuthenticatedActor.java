package com.jarl.seatforge.identity.application.port.in;

import java.util.Objects;
import java.util.Set;

public record AuthenticatedActor(ActorId id, Set<ActorRole> roles, Set<String> permissions) {

    public AuthenticatedActor {
        Objects.requireNonNull(id, "id must not be null");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null"));
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions must not be null"));
    }

    public boolean owns(ActorId ownerId) {
        return id.equals(ownerId);
    }

    public void requireOwnership(ActorId ownerId) {
        if (!owns(ownerId)) {
            throw new ActorAccessDeniedException();
        }
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasRole(ActorRole role) {
        return roles.contains(role);
    }
}
