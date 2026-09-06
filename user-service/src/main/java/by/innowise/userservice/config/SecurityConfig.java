package by.innowise.userservice.config;

import by.innowise.userservice.security.ResourceAuthorizationManager;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ResourceAuthorizationManager resourceAuthorizationManager
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/actuator/health/**")
                                .permitAll()
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/users"
                                )
                                .hasRole("SERVICE")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/users/{id}"
                                )
                                .hasAnyRole("ADMIN", "SERVICE")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users",
                                        "/api/v1/payment-cards"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/users/{id}/activate",
                                        "/api/v1/users/{id}/deactivate"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/email"
                                )
                                .hasRole("SERVICE")
                                .requestMatchers(
                                        "/api/v1/users/{id}",
                                        "/api/v1/users/{id}/details",
                                        "/api/v1/users/{userId}/payment-cards"
                                )
                                .access(
                                        resourceAuthorizationManager
                                                ::authorizeUser
                                )

                                .requestMatchers(
                                        "/api/v1/payment-cards/{id}",
                                        "/api/v1/payment-cards/{id}/activate",
                                        "/api/v1/payment-cards/{id}/deactivate"
                                )
                                .access(
                                        resourceAuthorizationManager
                                                ::authorizePaymentCard
                                )
                                .anyRequest()
                                .authenticated()
                )
                .exceptionHandling(exceptions ->
                        exceptions
                                .authenticationEntryPoint(
                                        (request, response, exception) ->
                                                writeProblem(
                                                        response,
                                                        HttpStatus.UNAUTHORIZED,
                                                        "Authentication is required"
                                                )
                                )
                                .accessDeniedHandler(
                                        (request, response, exception) ->
                                                writeProblem(
                                                        response,
                                                        HttpStatus.FORBIDDEN,
                                                        "Access denied"
                                                )
                                )
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                this::extractRealmRoles
        );

        return converter;
    }

    private Collection<GrantedAuthority> extractRealmRoles(
            Jwt jwt
    ) {
        Map<String, Object> realmAccess =
                jwt.getClaimAsMap("realm_access");

        if (realmAccess == null) {
            return List.of();
        }

        Object rolesValue = realmAccess.get("roles");

        if (!(rolesValue instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .<GrantedAuthority>map(role ->
                        new SimpleGrantedAuthority(
                                "ROLE_" + role.toUpperCase(
                                        Locale.ROOT
                                )
                        )
                )
                .toList();
    }

    private void writeProblem(
            HttpServletResponse response,
            HttpStatus status,
            String detail
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.getWriter().write(
                """
                {
                  "title": "%s",
                  "status": %d,
                  "detail": "%s"
                }
                """.formatted(
                        status.getReasonPhrase(),
                        status.value(),
                        detail
                )
        );
    }
}
