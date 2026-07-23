package by.innowise.userservice.mapper;

import by.innowise.userservice.dto.paymentcard.PaymentCardCreateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardUpdateDto;
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

class PaymentCardMapperTest {

    private final PaymentCardMapper paymentCardMapper = Mappers.getMapper(PaymentCardMapper.class);

    @Test
    void shouldMapCreateDtoToEntity() {
        PaymentCardCreateDto dto = new PaymentCardCreateDto("1111222233334444", "PAVEL KUPREICHIK", LocalDate.of(2030, 12, 31));

        PaymentCard paymentCard = paymentCardMapper.toEntity(dto);

        assertEquals("1111222233334444", paymentCard.getNumber());
        assertEquals("PAVEL KUPREICHIK", paymentCard.getHolder());
        assertEquals(LocalDate.of(2030, 12, 31), paymentCard.getExpirationDate());

        assertNull(paymentCard.getId());
        assertNull(paymentCard.getUser());
        assertNull(paymentCard.getCreatedAt());
        assertNull(paymentCard.getUpdatedAt());
        assertTrue(paymentCard.isActive());
    }

    @Test
    void shouldMapEntityToResponseDtoWithUserId() {
        PaymentCard paymentCard = createPaymentCard();

        PaymentCardResponseDto dto = paymentCardMapper.toDto(paymentCard);

        assertEquals(paymentCard.getId(), dto.id());
        assertEquals(paymentCard.getNumber(), dto.number());
        assertEquals(paymentCard.getHolder(), dto.holder());
        assertEquals(paymentCard.getExpirationDate(), dto.expirationDate());
        assertEquals(paymentCard.isActive(), dto.active());
        assertEquals(paymentCard.getUser().getId(), dto.userId());
        assertEquals(paymentCard.getCreatedAt(), dto.createdAt());
        assertEquals(paymentCard.getUpdatedAt(), dto.updatedAt());
    }

    @Test
    void shouldUpdateAllowedFieldsOnly() {
        PaymentCard paymentCard = createPaymentCard();

        Long originalId = paymentCard.getId();
        User originalUser = paymentCard.getUser();
        Instant originalCreatedAt = paymentCard.getCreatedAt();

        PaymentCardUpdateDto dto = new PaymentCardUpdateDto("5555666677778888", "UPDATED HOLDER", LocalDate.of(2032, 10, 20));

        paymentCardMapper.updateEntity(dto, paymentCard);

        assertEquals("5555666677778888", paymentCard.getNumber());
        assertEquals("UPDATED HOLDER", paymentCard.getHolder());
        assertEquals(LocalDate.of(2032, 10, 20), paymentCard.getExpirationDate());

        assertEquals(originalId, paymentCard.getId());
        assertSame(originalUser, paymentCard.getUser());
        assertEquals(originalCreatedAt, paymentCard.getCreatedAt());
        assertTrue(paymentCard.isActive());
    }

    private PaymentCard createPaymentCard() {
        User user = new User();
        user.setId(1L);

        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setId(10L);
        paymentCard.setNumber("1111222233334444");
        paymentCard.setHolder("PAVEL KUPREICHIK");
        paymentCard.setExpirationDate(LocalDate.of(2030, 12, 31));
        paymentCard.setActive(true);
        paymentCard.setUser(user);
        paymentCard.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        paymentCard.setUpdatedAt(Instant.parse("2026-01-02T10:00:00Z"));

        return paymentCard;
    }
}
