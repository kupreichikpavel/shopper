package by.innowise.userservice.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @Test
    void shouldSynchronizeBothSidesWhenAddingPaymentCard() {
        User user = new User();
        PaymentCard paymentCard = new PaymentCard();
        user.addPaymentCard(paymentCard);
        assertEquals(1, user.getPaymentCards().size());
        assertSame(paymentCard, user.getPaymentCards().get(0));
        assertSame(user, paymentCard.getUser());
    }

    @Test
    void shouldSynchronizeBothSidesWhenRemovingPaymentCard() {
        User user = new User();
        PaymentCard paymentCard = new PaymentCard();
        user.addPaymentCard(paymentCard);
        user.removePaymentCard(paymentCard);
        assertEquals(0, user.getPaymentCards().size());
        assertNull(paymentCard.getUser());
    }

    @Test
    void shouldRejectSixthPaymentCard() {
        User user = new User();
        for (int index = 0; index < User.MAX_PAYMENT_CARDS; index++) {
            user.addPaymentCard(new PaymentCard());
        }
        PaymentCard sixthPaymentCard = new PaymentCard();
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> user.addPaymentCard(sixthPaymentCard));
        assertEquals("User cannot have more than 5 payment cards", exception.getMessage());
        assertEquals(User.MAX_PAYMENT_CARDS, user.getPaymentCards().size());
        assertNull(sixthPaymentCard.getUser());
    }
}
