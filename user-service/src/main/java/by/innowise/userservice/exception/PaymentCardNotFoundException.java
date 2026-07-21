
package by.innowise.userservice.exception;

public class PaymentCardNotFoundException extends RuntimeException {

    public PaymentCardNotFoundException(Long paymentCardId) {
        super(
                "Payment card with id %d was not found"
                        .formatted(paymentCardId)
        );
    }
}