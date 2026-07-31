package by.innowise.userservice.service.impl;

import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.entity.PaymentCard;
import by.innowise.userservice.entity.User;
import by.innowise.userservice.exception.PaymentCardLimitExceededException;
import by.innowise.userservice.exception.PaymentCardNotFoundException;
import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.mapper.PaymentCardMapper;
import by.innowise.userservice.repository.PaymentCardRepository;
import by.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;
    private static final String CARD_NUMBER = "1111222233334444";

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userDetailsCache;

    @InjectMocks
    private PaymentCardServiceImpl paymentCardService;

    @Test
    void shouldCreatePaymentCard() {
        User user = createUser();
        PaymentCardRequestDto createDto = createCreateDto();
        PaymentCard paymentCard = createPaymentCard();
        PaymentCardResponseDto responseDto = createResponseDto();
        paymentCard.setUser(null);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(paymentCardRepository.countCardsByUserId(USER_ID))
                .thenReturn(0L);
        when(paymentCardMapper.toEntity(createDto))
                .thenReturn(paymentCard);
        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(responseDto);

        PaymentCardResponseDto result =
                paymentCardService.create(USER_ID, createDto);

        assertSame(responseDto, result);
        assertSame(user, paymentCard.getUser());
        assertEquals(1, user.getPaymentCards().size());
        verify(userRepository).findById(USER_ID);
        verify(paymentCardRepository).countCardsByUserId(USER_ID);
        verify(paymentCardMapper).toEntity(createDto);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void shouldRejectSixthPaymentCard() {
        User user = createUser();
        PaymentCardRequestDto createDto = createCreateDto();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(paymentCardRepository.countCardsByUserId(USER_ID))
                .thenReturn((long) User.MAX_PAYMENT_CARDS);

        assertThrows(
                PaymentCardLimitExceededException.class,
                () -> paymentCardService.create(USER_ID, createDto)
        );
        verify(paymentCardRepository).countCardsByUserId(USER_ID);
        verifyNoInteractions(paymentCardMapper);
        verify(paymentCardRepository, never())
                .save(any(PaymentCard.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingCardForMissingUser() {
        PaymentCardRequestDto createDto = createCreateDto();
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> paymentCardService.create(USER_ID, createDto)
        );
        verify(paymentCardRepository, never())
                .countCardsByUserId(USER_ID);
        verify(paymentCardRepository, never())
                .save(any(PaymentCard.class));
        verifyNoInteractions(paymentCardMapper);
    }

    @Test
    void shouldFindPaymentCardById() {
        PaymentCard paymentCard = createPaymentCard();
        PaymentCardResponseDto responseDto = createResponseDto();
        when(paymentCardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(paymentCard));
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(responseDto);

        PaymentCardResponseDto result =
                paymentCardService.findById(CARD_ID);

        assertSame(responseDto, result);
        verify(paymentCardRepository).findById(CARD_ID);
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void shouldThrowExceptionWhenPaymentCardNotFound() {
        when(paymentCardRepository.findById(CARD_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentCardNotFoundException.class,
                () -> paymentCardService.findById(CARD_ID)
        );
        verifyNoInteractions(paymentCardMapper);
    }

    @Test
    void shouldFindAllPaymentCardsByUserId() {
        User user = createUser();
        PaymentCard firstCard = createPaymentCard();
        PaymentCard secondCard = createPaymentCard();
        secondCard.setId(11L);
        secondCard.setNumber("5555666677778888");
        PaymentCardResponseDto firstResponse = createResponseDto();
        PaymentCardResponseDto secondResponse =
                new PaymentCardResponseDto(
                        11L,
                        "5555666677778888",
                        "PAVEL KUPREICHIK",
                        LocalDate.of(2030, 12, 31),
                        true,
                        USER_ID,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-01T10:00:00Z")
                );
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(paymentCardRepository.findAllByUser_Id(USER_ID))
                .thenReturn(List.of(firstCard, secondCard));
        when(paymentCardMapper.toDto(firstCard))
                .thenReturn(firstResponse);
        when(paymentCardMapper.toDto(secondCard))
                .thenReturn(secondResponse);

        List<PaymentCardResponseDto> result =
                paymentCardService.findAllByUserId(USER_ID);

        assertEquals(2, result.size());
        assertSame(firstResponse, result.get(0));
        assertSame(secondResponse, result.get(1));
        verify(userRepository).findById(USER_ID);
        verify(paymentCardRepository).findAllByUser_Id(USER_ID);
    }

    @Test
    void shouldThrowExceptionWhenFindingCardsForMissingUser() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> paymentCardService.findAllByUserId(USER_ID)
        );
        verify(paymentCardRepository, never())
                .findAllByUser_Id(USER_ID);
        verifyNoInteractions(paymentCardMapper);
    }

    @Test
    void shouldFindCardsWithFilteringAndPagination() {
        PaymentCard paymentCard = createPaymentCard();
        PaymentCardResponseDto responseDto = createResponseDto();
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> cards =
                new PageImpl<>(List.of(paymentCard), pageable, 1);
        when(paymentCardRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(cards);
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(responseDto);

        Page<PaymentCardResponseDto> result =
                paymentCardService.findAll("Pav", "Kup", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertSame(responseDto, result.getContent().getFirst());
        verify(paymentCardRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void shouldUpdatePaymentCard() {
        PaymentCard paymentCard = createPaymentCard();
        PaymentCardRequestDto updateDto = createUpdateDto();
        PaymentCardResponseDto responseDto = createResponseDto();
        when(paymentCardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(paymentCard));
        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(responseDto);

        PaymentCardResponseDto result =
                paymentCardService.update(CARD_ID, updateDto);

        assertSame(responseDto, result);
        verify(paymentCardMapper).updateEntity(updateDto, paymentCard);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void shouldChangePaymentCardActiveStatus() {
        PaymentCard paymentCard = createPaymentCard();
        paymentCard.setActive(false);
        PaymentCardResponseDto responseDto =
                new PaymentCardResponseDto(
                        CARD_ID,
                        CARD_NUMBER,
                        paymentCard.getHolder(),
                        paymentCard.getExpirationDate(),
                        false,
                        USER_ID,
                        paymentCard.getCreatedAt(),
                        paymentCard.getUpdatedAt()
                );
        when(paymentCardRepository.updateActiveById(CARD_ID, false))
                .thenReturn(1);
        when(paymentCardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(paymentCard));
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(responseDto);

        PaymentCardResponseDto result =
                paymentCardService.setActive(CARD_ID, false);

        assertFalse(result.active());
        verify(paymentCardRepository)
                .updateActiveById(CARD_ID, false);
        verify(paymentCardRepository).findById(CARD_ID);
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void shouldThrowExceptionWhenChangingMissingCard() {
        when(paymentCardRepository.updateActiveById(CARD_ID, false))
                .thenReturn(0);

        assertThrows(
                PaymentCardNotFoundException.class,
                () -> paymentCardService.setActive(CARD_ID, false)
        );
        verify(paymentCardRepository, never()).findById(CARD_ID);
        verifyNoInteractions(paymentCardMapper);
    }

    @Test
    void shouldDeletePaymentCard() {
        PaymentCard paymentCard = createPaymentCard();
        when(paymentCardRepository.findById(CARD_ID))
                .thenReturn(Optional.of(paymentCard));

        paymentCardService.delete(CARD_ID);

        verify(paymentCardRepository).findById(CARD_ID);
        verify(paymentCardRepository).delete(paymentCard);
    }

    private PaymentCardRequestDto createCreateDto() {
        return new PaymentCardRequestDto(
                CARD_NUMBER,
                "PAVEL KUPREICHIK",
                LocalDate.of(2030, 12, 31)
        );
    }

    private PaymentCardRequestDto createUpdateDto() {
        return new PaymentCardRequestDto(
                CARD_NUMBER,
                "UPDATED HOLDER",
                LocalDate.of(2032, 12, 31)
        );
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Pavel");
        user.setSurname("Kupreichik");
        user.setBirthDate(LocalDate.of(2006, 1, 1));
        user.setEmail("pavel@example.com");
        user.setActive(true);
        return user;
    }

    private PaymentCard createPaymentCard() {
        User user = createUser();
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setId(CARD_ID);
        paymentCard.setNumber(CARD_NUMBER);
        paymentCard.setHolder("PAVEL KUPREICHIK");
        paymentCard.setExpirationDate(
                LocalDate.of(2030, 12, 31)
        );
        paymentCard.setActive(true);
        paymentCard.setCreatedAt(
                Instant.parse("2026-01-01T10:00:00Z")
        );
        paymentCard.setUpdatedAt(
                Instant.parse("2026-01-01T10:00:00Z")
        );
        paymentCard.setUser(user);
        return paymentCard;
    }

    private PaymentCardResponseDto createResponseDto() {
        return new PaymentCardResponseDto(
                CARD_ID,
                CARD_NUMBER,
                "PAVEL KUPREICHIK",
                LocalDate.of(2030, 12, 31),
                true,
                USER_ID,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }
}