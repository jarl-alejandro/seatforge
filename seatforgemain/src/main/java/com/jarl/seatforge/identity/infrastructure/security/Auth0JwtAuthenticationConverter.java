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
        Set<String> permissions = permissions(jwt.getClaim("permissions"));
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

    private Set<String> permissions(Object claim) {
        if (!(claim instanceof Collection<?> values)) {
            return Set.of();
        }

        Set<String> permissions = new LinkedHashSet<>();
        for (Object value : values) {
            if (value instanceof String permission && !permission.isBlank()) {
                permissions.add(permission);
            }
        }
        return Set.copyOf(permissions);
    }
}
