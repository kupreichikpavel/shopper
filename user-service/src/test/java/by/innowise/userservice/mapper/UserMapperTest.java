package by.innowise.userservice.mapper;

import by.innowise.userservice.dto.user.UserCreateDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.dto.user.UserUpdateDto;
import by.innowise.userservice.entity.PaymentCard;
import by.innowise.userservice.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapCreateDtoToEntity() {
        UserCreateDto dto = new UserCreateDto("Pavel", "Kupreichik", LocalDate.of(2006, 1, 1), "pavel@example.com");

        User user = userMapper.toEntity(dto);

        assertEquals("Pavel", user.getName());
        assertEquals("Kupreichik", user.getSurname());
        assertEquals(LocalDate.of(2006, 1, 1), user.getBirthDate());
        assertEquals("pavel@example.com", user.getEmail());

        assertNull(user.getId());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
        assertTrue(user.isActive());
        assertTrue(user.getPaymentCards().isEmpty());
    }

    @Test
    void shouldMapEntityToResponseDto() {
        User user = createUser();

        UserResponseDto dto = userMapper.toDto(user);

        assertEquals(user.getId(), dto.id());
        assertEquals(user.getName(), dto.name());
        assertEquals(user.getSurname(), dto.surname());
        assertEquals(user.getBirthDate(), dto.birthDate());
        assertEquals(user.getEmail(), dto.email());
        assertEquals(user.isActive(), dto.active());
        assertEquals(user.getCreatedAt(), dto.createdAt());
        assertEquals(user.getUpdatedAt(), dto.updatedAt());
    }

    @Test
    void shouldUpdateAllowedFieldsOnly() {
        User user = createUser();
        PaymentCard paymentCard = new PaymentCard();
        user.addPaymentCard(paymentCard);

        Long originalId = user.getId();
        Instant originalCreatedAt = user.getCreatedAt();

        UserUpdateDto dto = new UserUpdateDto("Alexey", "Ivanov", LocalDate.of(2005, 5, 10), "alexey@example.com");

        userMapper.updateEntity(dto, user);

        assertEquals("Alexey", user.getName());
        assertEquals("Ivanov", user.getSurname());
        assertEquals(LocalDate.of(2005, 5, 10), user.getBirthDate());
        assertEquals("alexey@example.com", user.getEmail());

        assertEquals(originalId, user.getId());
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertTrue(user.isActive());

        assertEquals(1, user.getPaymentCards().size());
        assertSame(paymentCard, user.getPaymentCards().getFirst());
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Pavel");
        user.setSurname("Kupreichik");
        user.setBirthDate(LocalDate.of(2006, 1, 1));
        user.setEmail("pavel@example.com");
        user.setActive(true);
        user.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));
        return user;
    }
}
