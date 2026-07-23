package by.innowise.userservice.repository;

import by.innowise.userservice.TestcontainersConfiguration;
import by.innowise.userservice.entity.PaymentCard;
import by.innowise.userservice.entity.User;
import by.innowise.userservice.specification.UserSpecifications;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static by.innowise.userservice.testutil.TestDataFactory.createPaymentCard;
import static by.innowise.userservice.testutil.TestDataFactory.createUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindUserByEmailIgnoringCase() {
        User user = createUser("pavel@example.com");
        userRepository.saveAndFlush(user);
        User foundUser = userRepository.findByEmailIgnoreCase("PAVEL@EXAMPLE.COM").orElseThrow();
        assertEquals(user.getId(), foundUser.getId());
    }

    @Test
    void shouldDeactivateUser() {
        User user = createUser("active.user@example.com");
        userRepository.saveAndFlush(user);
        int updatedRows = userRepository.updateActiveById(user.getId(), false);
        entityManager.clear();
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertEquals(1, updatedRows);
        assertFalse(updatedUser.isActive());
    }

    @Test
    void shouldUpdateUserById() {
        User user = createUser("update.user@example.com");
        userRepository.saveAndFlush(user);
        Long userId = user.getId();
        User userToUpdate = userRepository.findById(userId).orElseThrow();
        userToUpdate.setName("Updated");
        userToUpdate.setSurname("User");
        userRepository.saveAndFlush(userToUpdate);
        entityManager.clear();
        User updatedUser = userRepository.findById(userId).orElseThrow();

        assertEquals("Updated", updatedUser.getName());
        assertEquals("User", updatedUser.getSurname());
    }

    @Test
    void shouldFilterUsersByNameAndSurnameWithPagination() {
        User pavelKupreichik = createUser("pavel.kupreichik@example.com");
        pavelKupreichik.setName("Pavel");
        pavelKupreichik.setSurname("Kupreichik");
        User annaKupreichik = createUser("anna.kupreichik@example.com");
        annaKupreichik.setName("Anna");
        annaKupreichik.setSurname("Kupreichik");
        User pavelIvanov = createUser("pavel.ivanov@example.com");
        pavelIvanov.setName("Pavel");
        pavelIvanov.setSurname("Ivanov");
        userRepository.saveAllAndFlush(List.of(pavelKupreichik, annaKupreichik, pavelIvanov));
        Page<User> firstPage = userRepository.findAll(UserSpecifications.byNameAndSurname(null, "KUP"),
                PageRequest.of(0, 1, Sort.by("email").ascending()));
        Page<User> secondPage = userRepository.findAll(UserSpecifications.byNameAndSurname(null, "kup"),
                PageRequest.of(1, 1, Sort.by("email").ascending()));
        Page<User> filteredByBothFields = userRepository.findAll(UserSpecifications.byNameAndSurname("pav", "kup"),
                PageRequest.of(0, 10));

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(1, firstPage.getContent().size());
        assertTrue(firstPage.hasNext());
        assertEquals(1, secondPage.getContent().size());
        assertFalse(secondPage.hasNext());
        assertEquals(1, filteredByBothFields.getTotalElements());
        assertEquals("pavel.kupreichik@example.com", filteredByBothFields.getContent().get(0).getEmail());
    }

    @Test
    void shouldDeletePaymentCardsWhenUserDeleted() {
        User user = createUser("cascade@example.com");
        PaymentCard paymentCard = createPaymentCard("4444333322221111");

        user.addPaymentCard(paymentCard);
        userRepository.saveAndFlush(user);

        Long userId = user.getId();
        Long paymentCardId = paymentCard.getId();

        userRepository.delete(user);
        userRepository.flush();
        entityManager.clear();

        assertFalse(userRepository.existsById(userId));
        assertFalse(paymentCardRepository.existsById(paymentCardId));
    }
}
