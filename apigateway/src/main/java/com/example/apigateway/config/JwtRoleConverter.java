package com.example.apigateway.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class JwtRoleConverter {
    public static Collection<GrantedAuthority> getGrantedAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null) {
            return Collections.emptyList();
        }

        Object rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof List<?> roles)) {
            return Collections.emptyList();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Object role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
        }
        return authorities;
    }
}
