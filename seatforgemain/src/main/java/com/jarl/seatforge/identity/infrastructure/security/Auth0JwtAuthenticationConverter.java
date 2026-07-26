package com.jarl.seatforge.identity.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

final class Auth0JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> permissions = permissions(jwt);
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        permissions.stream().map(SimpleGrantedAuthority::new).forEach(authorities::add);

        if (permissions.stream().anyMatch(SeatForgePermissions.ORGANIZER::contains)) {
            authorities.add(new SimpleGrantedAuthority(SeatForgePermissions.ROLE_ORGANIZER));
        }
        if (permissions.stream().anyMatch(SeatForgePermissions.BUYER::contains)) {
            authorities.add(new SimpleGrantedAuthority(SeatForgePermissions.ROLE_BUYER));
        }

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Set<String> permissions(Jwt jwt) {
        Set<String> permissions = new LinkedHashSet<>();
        Object permissionsClaim = jwt.getClaim("permissions");
        if (permissionsClaim instanceof Collection<?> values) {
            for (Object value : values) {
                if (value instanceof String permission) {
                    addKnownPermission(permissions, permission);
                }
            }
        }

        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim instanceof String scope) {
            for (String permission : scope.trim().split("\\s+")) {
                addKnownPermission(permissions, permission);
            }
        }
        return Set.copyOf(permissions);
    }

    private void addKnownPermission(Set<String> permissions, String permission) {
        if (SeatForgePermissions.ALL.contains(permission)) {
            permissions.add(permission);
        }
    }
}
