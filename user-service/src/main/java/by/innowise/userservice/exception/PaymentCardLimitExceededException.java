package by.innowise.userservice.exception;

public class PaymentCardLimitExceededException extends RuntimeException {

    public PaymentCardLimitExceededException(Long userId) {
        super(
                "User with id %d cannot have more than 5 payment cards"
                        .formatted(userId)
        );
    }
}