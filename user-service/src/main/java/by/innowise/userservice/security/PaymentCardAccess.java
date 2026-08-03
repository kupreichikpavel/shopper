package by.innowise.userservice.security;

import by.innowise.userservice.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCardAccess {

    private final PaymentCardRepository paymentCardRepository;
    private final UserAccess userAccess;

    public boolean isOwner(
            Long paymentCardId,
            Authentication authentication
    ) {
        if (paymentCardId == null) {
            return false;
        }

        return userAccess.findUserId(authentication)
                .map(userId ->
                        paymentCardRepository
                                .existsByIdAndUser_Id(
                                        paymentCardId,
                                        userId
                                )
                )
                .orElse(false);
    }
}
