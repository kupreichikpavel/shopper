package by.innowise.userservice.controller;

import by.innowise.userservice.dto.paymentcard.PaymentCardCreateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardUpdateDto;
import by.innowise.userservice.exception.PaymentCardLimitExceededException;
import by.innowise.userservice.exception.PaymentCardNotFoundException;
import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.exception.handler.GlobalExceptionHandler;
import by.innowise.userservice.service.PaymentCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentCardController.class)
@Import(GlobalExceptionHandler.class)
class PaymentCardControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentCardService paymentCardService;

    @Test
    void shouldCreatePaymentCard() throws Exception {
        PaymentCardResponseDto responseDto = createResponseDto();
        when(paymentCardService.create(eq(USER_ID), any(PaymentCardCreateDto.class))).thenReturn(responseDto);
        mockMvc.perform(post("/api/v1/users/{userId}/payment-cards", USER_ID).contentType("application/json").content("""
                        {
                          "number": "1111222233334444",
                          "holder": "PAVEL KUPREICHIK",
                          "expirationDate": "2030-12-31"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value(CARD_ID)).andExpect(jsonPath("$.number").value("1111222233334444"))
                .andExpect(jsonPath("$.holder").value("PAVEL KUPREICHIK"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value(USER_ID));

        verify(paymentCardService).create(eq(USER_ID), any(PaymentCardCreateDto.class));
    }

    @Test
    void shouldFindPaymentCardById() throws Exception {
        when(paymentCardService.findById(CARD_ID)).thenReturn(createResponseDto());
        mockMvc.perform(get("/api/v1/payment-cards/{id}", CARD_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(CARD_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID));
        verify(paymentCardService).findById(CARD_ID);
    }

    @Test
    void shouldFindAllPaymentCardsByUserId() throws Exception {
        when(paymentCardService.findAllByUserId(USER_ID)).thenReturn(List.of(createResponseDto()));
        mockMvc.perform(get("/api/v1/users/{userId}/payment-cards", USER_ID))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(CARD_ID))
                .andExpect(jsonPath("$[0].userId").value(USER_ID));
        verify(paymentCardService).findAllByUserId(USER_ID);
    }

    @Test
    void shouldFindPaymentCardsWithFiltersAndPagination() throws Exception {
        when(paymentCardService.findAll(eq("Pav"), eq("Kup"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(createResponseDto())));
        mockMvc.perform(get("/api/v1/payment-cards")
                        .param("ownerName", "Pav")
                        .param("ownerSurname", "Kup")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(CARD_ID))
                .andExpect(jsonPath("$.content[0].userId").value(USER_ID));
        verify(paymentCardService).findAll(eq("Pav"), eq("Kup"), any(Pageable.class));
    }

    @Test
    void shouldUpdatePaymentCard() throws Exception {
        when(paymentCardService.update(eq(CARD_ID), any(PaymentCardUpdateDto.class))).thenReturn(createResponseDto());
        mockMvc.perform(put("/api/v1/payment-cards/{id}", CARD_ID).contentType("application/json").content("""
                {
                  "number": "1111222233334444",
                  "holder": "PAVEL KUPREICHIK",
                  "expirationDate": "2030-12-31"
                }
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(CARD_ID));

        verify(paymentCardService).update(eq(CARD_ID), any(PaymentCardUpdateDto.class));
    }

    @Test
    void shouldDeactivatePaymentCard() throws Exception {
        PaymentCardResponseDto inactiveCard = new PaymentCardResponseDto(CARD_ID, "1111222233334444", "PAVEL KUPREICHIK",
                LocalDate.of(2030, 12, 31), false, USER_ID,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z"));
        when(paymentCardService.setActive(CARD_ID, false)).thenReturn(inactiveCard);
        mockMvc.perform(patch("/api/v1/payment-cards/{id}/active", CARD_ID).contentType("application/json").content("""
                {
                  "active": false
                }
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        verify(paymentCardService).setActive(CARD_ID, false);
    }

    @Test
    void shouldDeletePaymentCard() throws Exception {
        mockMvc.perform(delete("/api/v1/payment-cards/{id}", CARD_ID)).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(paymentCardService).delete(CARD_ID);
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/payment-cards", USER_ID)
                        .contentType("application/json")
                        .content("""
                                {
                                  "number": "123",
                                  "holder": "",
                                  "expirationDate": "2020-01-01"
                                }
                                """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.number").exists()).andExpect(jsonPath("$.fieldErrors.holder").exists())
                .andExpect(jsonPath("$.fieldErrors.expirationDate").exists());
        verify(paymentCardService, never()).create(eq(USER_ID), any(PaymentCardCreateDto.class));
    }

    @Test
    void shouldReturnNotFoundWhenPaymentCardDoesNotExist() throws Exception {
        when(paymentCardService.findById(999L)).thenThrow(new PaymentCardNotFoundException(999L));
        mockMvc.perform(get("/api/v1/payment-cards/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Payment card with id 999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/payment-cards/999"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        when(paymentCardService.create(eq(999L), any(PaymentCardCreateDto.class))).thenThrow(new UserNotFoundException(999L));
        mockMvc.perform(post("/api/v1/users/{userId}/payment-cards", 999L).contentType("application/json").content("""
                        {
                          "number": "1111222233334444",
                          "holder": "PAVEL KUPREICHIK",
                          "expirationDate": "2030-12-31"
                        }
                        """)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with id 999 was not found"));
    }

    @Test
    void shouldReturnConflictWhenCardLimitExceeded() throws Exception {
        when(paymentCardService.create(eq(USER_ID), any(PaymentCardCreateDto.class))).thenThrow(new PaymentCardLimitExceededException(USER_ID));
        mockMvc.perform(post("/api/v1/users/{userId}/payment-cards", USER_ID).contentType("application/json").content("""
                        {
                          "number": "1111222233334444",
                          "holder": "PAVEL KUPREICHIK",
                          "expirationDate": "2030-12-31"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldRejectNonPositivePaymentCardId() throws Exception {
        mockMvc.perform(get("/api/v1/payment-cards/{id}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(paymentCardService, never()).findById(0L);
    }

    private PaymentCardResponseDto createResponseDto() {
        return new PaymentCardResponseDto(
                CARD_ID,
                "1111222233334444",
                "PAVEL KUPREICHIK",
                LocalDate.of(2030, 12, 31),
                true,
                USER_ID,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z")
        );
    }
}
