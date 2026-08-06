package by.innowise.userservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class UserAccess {

    private static final String USER_ID_CLAIM = "userId";

    public boolean isOwner(
            Long userId,
            Authentication authentication
    ) {
        if (userId == null) {
            return false;
        }

        return findUserId(authentication)
                .map(userId::equals)
                .orElse(false);
    }

    public Optional<Long> findUserId(
            Authentication authentication
    ) {
        if (!(authentication
                instanceof JwtAuthenticationToken jwtAuthentication)) {
            return Optional.empty();
        }

        Object claimValue = jwtAuthentication
                .getToken()
                .getClaim(USER_ID_CLAIM);

        return parseUserId(claimValue);
    }

    private Optional<Long> parseUserId(Object claimValue) {
        if (claimValue instanceof Number number) {
            return Optional.of(number.longValue());
        }

        if (claimValue instanceof String value) {
            try {
                return Optional.of(
                        Long.parseLong(value.trim())
                );
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        if (claimValue instanceof Collection<?> values) {
            return values.stream()
                    .map(this::parseUserId)
                    .flatMap(Optional::stream)
                    .findFirst();
        }

        return Optional.empty();
    }
}
