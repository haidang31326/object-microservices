package com.example.apigateway.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class JwtRoleConverter {
    public static Collection<GrantedAuthority> getGrantedAuthorities(Jwt jwt) {
        Map<String , Object> realmAccess = jwt.getClaim("realm_access");

        if(realmAccess == null) {
            return Collections.emptyList();
        }
        List<String> roles = (List<String>) realmAccess.get("roles");

        List<GrantedAuthority> authorities = new ArrayList<>();

        for(String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
return authorities;
    }
}
