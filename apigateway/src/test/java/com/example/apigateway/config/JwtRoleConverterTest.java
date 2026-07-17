package com.example.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRoleConverterTest {

    @Test
    void getGrantedAuthorities_mapsRealmRolesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER", "ADMIN")))
                .build();

        Collection<GrantedAuthority> authorities = JwtRoleConverter.getGrantedAuthorities(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void getGrantedAuthorities_returnsEmptyListWhenRealmAccessIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .build();

        Collection<GrantedAuthority> authorities = JwtRoleConverter.getGrantedAuthorities(jwt);

        assertThat(authorities).isEmpty();
    }
}
