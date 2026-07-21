package by.innowise.userservice.repository;

import by.innowise.userservice.TestcontainersConfiguration;
import by.innowise.userservice.entity.PaymentCard;
import by.innowise.userservice.entity.User;
import by.innowise.userservice.specification.PaymentCardSpecifications;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static by.innowise.userservice.testutil.TestDataFactory.createPaymentCard;
import static by.innowise.userservice.testutil.TestDataFactory.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PaymentCardRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveUserWithPaymentCardByCascade() {
        User user = createUser("cascade.save@example.com");
        PaymentCard paymentCard =
                createPaymentCard("1111222233334444");

        user.addPaymentCard(paymentCard);

        User savedUser = userRepository.saveAndFlush(user);

        assertNotNull(savedUser.getId());
        assertNotNull(paymentCard.getId());

        assertEquals(
                savedUser.getId(),
                paymentCard.getUser().getId()
        );

        List<PaymentCard> cards =
                paymentCardRepository.findAllByUser_Id(
                        savedUser.getId()
                );

        assertEquals(1, cards.size());
        assertEquals(
                "1111222233334444",
                cards.get(0).getNumber()
        );
    }

    @Test
    void shouldFindAndCountCardsByUserId() {
        User user = createUser("cards@example.com");

        user.addPaymentCard(
                createPaymentCard("1111222233334444")
        );

        user.addPaymentCard(
                createPaymentCard("5555666677778888")
        );

        userRepository.saveAndFlush(user);

        List<PaymentCard> cards =
                paymentCardRepository.findAllByUser_Id(
                        user.getId()
                );

        long cardsCount =
                paymentCardRepository.countCardsByUserId(
                        user.getId()
                );

        assertEquals(2, cards.size());
        assertEquals(2L, cardsCount);
    }

    @Test
    void shouldDeactivatePaymentCard() {
        User user = createUser("active.card@example.com");
        PaymentCard paymentCard =
                createPaymentCard("9999000011112222");

        user.addPaymentCard(paymentCard);
        userRepository.saveAndFlush(user);

        int updatedRows =
                paymentCardRepository.updateActiveById(
                        paymentCard.getId(),
                        false
                );

        entityManager.clear();

        PaymentCard updatedCard = paymentCardRepository
                .findById(paymentCard.getId())
                .orElseThrow();

        assertEquals(1, updatedRows);
        assertFalse(updatedCard.isActive());
    }

    @Test
    void shouldUpdatePaymentCardById() {
        User user = createUser("update.card@example.com");
        PaymentCard paymentCard =
                createPaymentCard("1111000022223333");

        user.addPaymentCard(paymentCard);
        userRepository.saveAndFlush(user);

        Long paymentCardId = paymentCard.getId();

        PaymentCard cardToUpdate = paymentCardRepository
                .findById(paymentCardId)
                .orElseThrow();

        cardToUpdate.setHolder("UPDATED USER");
        cardToUpdate.setExpirationDate(
                LocalDate.of(2032, 12, 31)
        );

        paymentCardRepository.saveAndFlush(cardToUpdate);
        entityManager.clear();

        PaymentCard updatedCard = paymentCardRepository
                .findById(paymentCardId)
                .orElseThrow();

        assertEquals(
                "UPDATED USER",
                updatedCard.getHolder()
        );

        assertEquals(
                LocalDate.of(2032, 12, 31),
                updatedCard.getExpirationDate()
        );
    }

    @Test
    void shouldFilterPaymentCardsByOwnerWithPagination() {
        User pavel = createUser("owner.pavel@example.com");
        pavel.setName("Pavel");
        pavel.setSurname("Kupreichik");

        pavel.addPaymentCard(
                createPaymentCard("1111222233334444")
        );

        pavel.addPaymentCard(
                createPaymentCard("2222333344445555")
        );

        User anna = createUser("owner.anna@example.com");
        anna.setName("Anna");
        anna.setSurname("Kupreichik");

        anna.addPaymentCard(
                createPaymentCard("9999888877776666")
        );

        userRepository.saveAllAndFlush(
                List.of(pavel, anna)
        );

        Page<PaymentCard> firstPage =
                paymentCardRepository.findAll(
                        PaymentCardSpecifications
                                .byOwnerNameAndSurname(
                                        "PAV",
                                        "kup"
                                ),
                        PageRequest.of(
                                0,
                                1,
                                Sort.by("number").ascending()
                        )
                );

        Page<PaymentCard> secondPage =
                paymentCardRepository.findAll(
                        PaymentCardSpecifications
                                .byOwnerNameAndSurname(
                                        "pav",
                                        "kup"
                                ),
                        PageRequest.of(
                                1,
                                1,
                                Sort.by("number").ascending()
                        )
                );

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(1, firstPage.getContent().size());
        assertTrue(firstPage.hasNext());

        assertEquals(
                "Pavel",
                firstPage.getContent().get(0)
                        .getUser().getName()
        );

        assertEquals(1, secondPage.getContent().size());
        assertFalse(secondPage.hasNext());

        assertEquals(
                "Pavel",
                secondPage.getContent().get(0)
                        .getUser().getName()
        );
    }

    @Test
    void shouldDeletePaymentCardRemovedFromUser() {
        User user = createUser("orphan@example.com");
        PaymentCard paymentCard =
                createPaymentCard("5555000011112222");

        user.addPaymentCard(paymentCard);
        userRepository.saveAndFlush(user);

        Long paymentCardId = paymentCard.getId();

        user.removePaymentCard(paymentCard);
        userRepository.saveAndFlush(user);

        entityManager.clear();

        assertFalse(
                paymentCardRepository.existsById(paymentCardId)
        );
    }
}