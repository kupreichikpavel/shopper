package by.innowise.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ResourceAuthorizationManager {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserAccess userAccess;
    private final PaymentCardAccess paymentCardAccess;

    public AuthorizationResult authorizeUser(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        Long userId = extractId(
                context,
                "userId",
                "id"
        );

        return authorize(
                authentication,
                userId,
                userAccess::isOwner
        );
    }

    public AuthorizationResult authorizePaymentCard(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        Long paymentCardId = extractId(
                context,
                "id"
        );

        return authorize(
                authentication,
                paymentCardId,
                paymentCardAccess::isOwner
        );
    }

    private AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            Long resourceId,
            BiPredicate<Long, Authentication> ownershipCheck
    ) {
        Authentication currentAuthentication = authentication.get();

        if (hasAuthority(currentAuthentication, ROLE_ADMIN)) {
            return new AuthorizationDecision(true);
        }

        if (!hasAuthority(currentAuthentication, ROLE_USER)
                || resourceId == null) {
            return new AuthorizationDecision(false);
        }

        return new AuthorizationDecision(
                ownershipCheck.test(
                        resourceId,
                        currentAuthentication
                )
        );
    }

    private Long extractId(
            RequestAuthorizationContext context,
            String... variableNames
    ) {
        for (String variableName : variableNames) {
            String value = context.getVariables().get(variableName);

            if (value == null) {
                continue;
            }

            try {
                return Long.valueOf(value);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private boolean hasAuthority(
            Authentication authentication,
            String requiredAuthority
    ) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        requiredAuthority.equals(
                                authority.getAuthority()
                        )
                );
    }
}
