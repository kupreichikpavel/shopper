package by.innowise.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PaymentCardRequestAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final PaymentCardAccess paymentCardAccess;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        Authentication currentAuthentication = authentication.get();

        if (hasAuthority(currentAuthentication, ROLE_ADMIN)) {
            return new AuthorizationDecision(true);
        }

        if (!hasAuthority(currentAuthentication, ROLE_USER)) {
            return new AuthorizationDecision(false);
        }

        Long paymentCardId = extractPaymentCardId(context);

        return new AuthorizationDecision(
                paymentCardAccess.isOwner(
                        paymentCardId,
                        currentAuthentication
                )
        );
    }

    private Long extractPaymentCardId(
            RequestAuthorizationContext context
    ) {
        String value = context.getVariables().get("id");

        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
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
