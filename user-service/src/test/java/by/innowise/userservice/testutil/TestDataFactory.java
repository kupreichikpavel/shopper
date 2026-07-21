package by.innowise.userservice.testutil;

import by.innowise.userservice.entity.PaymentCard;
import by.innowise.userservice.entity.User;

import java.time.LocalDate;

public final class TestDataFactory {

    private static final LocalDate DEFAULT_BIRTH_DATE =
            LocalDate.of(2006, 1, 1);

    private static final LocalDate DEFAULT_EXPIRATION_DATE =
            LocalDate.of(2030, 12, 31);

    private TestDataFactory() {
    }

    public static User createUser(String email) {
        User user = new User();
        user.setName("Pavel");
        user.setSurname("Kupreichik");
        user.setBirthDate(DEFAULT_BIRTH_DATE);
        user.setEmail(email);
        user.setActive(true);
        return user;
    }

    public static PaymentCard createPaymentCard(String number) {
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setNumber(number);
        paymentCard.setHolder("PAVEL KUPREICHIK");
        paymentCard.setExpirationDate(DEFAULT_EXPIRATION_DATE);
        paymentCard.setActive(true);
        return paymentCard;
    }
}