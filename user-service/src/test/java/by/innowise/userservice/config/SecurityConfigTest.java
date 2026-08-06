package by.innowise.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig =
            new SecurityConfig();

    @Test
    void shouldConvertKeycloakRealmRolesToAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .claim(
                        "realm_access",
                        Map.of(
                                "roles",
                                List.of(
                                        "USER",
                                        "admin",
                                        "offline_access"
                                )
                        )
                )
                .build();

        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter();

        AbstractAuthenticationToken authentication =
                converter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(extractRoleAuthorities(authentication))
                .containsExactlyInAnyOrder(
                        "ROLE_USER",
                        "ROLE_ADMIN",
                        "ROLE_OFFLINE_ACCESS"
                );
    }

    @Test
    void shouldReturnNoRoleAuthoritiesWithoutRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();

        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter();

        AbstractAuthenticationToken authentication =
                converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(extractRoleAuthorities(authentication))
                .isEmpty();
    }

    @Test
    void shouldIgnoreInvalidRoleValues() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .claim(
                        "realm_access",
                        Map.of(
                                "roles",
                                List.of(
                                        "USER",
                                        123,
                                        true
                                )
                        )
                )
                .build();

        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter();

        AbstractAuthenticationToken authentication =
                converter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(extractRoleAuthorities(authentication))
                .containsExactly("ROLE_USER");
    }

    private Set<String> extractRoleAuthorities(
            AbstractAuthenticationToken authentication
    ) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority ->
                        authority.startsWith("ROLE_")
                )
                .collect(Collectors.toSet());
    }
}
