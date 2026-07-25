package by.innowise.userservice.controller;

import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.exception.PaymentCardLimitExceededException;
import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.exception.handler.GlobalExceptionHandler;
import by.innowise.userservice.service.PaymentCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPaymentCardController.class)
@Import(GlobalExceptionHandler.class)
class UserPaymentCardControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentCardService paymentCardService;

    @SuppressWarnings("unused")
    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void shouldCreatePaymentCard() throws Exception {
        when(paymentCardService.create(
                eq(USER_ID),
                any(PaymentCardRequestDto.class)
        )).thenReturn(createResponseDto());

        mockMvc.perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(validRequestJson())
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(CARD_ID))
                .andExpect(
                        jsonPath("$.number")
                                .value("1111222233334444")
                )
                .andExpect(
                        jsonPath("$.holder")
                                .value("PAVEL KUPREICHIK")
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(USER_ID)
                );

        verify(paymentCardService).create(
                eq(USER_ID),
                any(PaymentCardRequestDto.class)
        );
    }

    @Test
    void shouldFindAllPaymentCardsByUserId()
            throws Exception {

        when(paymentCardService.findAllByUserId(USER_ID))
                .thenReturn(
                        List.of(createResponseDto())
                );

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(CARD_ID)
                )
                .andExpect(
                        jsonPath("$[0].userId")
                                .value(USER_ID)
                );

        verify(paymentCardService)
                .findAllByUserId(USER_ID);
    }

    @Test
    void shouldRejectInvalidCreateRequest()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "number": "123",
                                          "holder": "",
                                          "expirationDate": "2020-01-01"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Request validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.fieldErrors.number")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.holder")
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.fieldErrors.expirationDate"
                        ).exists()
                );

        verify(paymentCardService, never()).create(
                eq(USER_ID),
                any(PaymentCardRequestDto.class)
        );
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist()
            throws Exception {

        when(paymentCardService.create(
                eq(999L),
                any(PaymentCardRequestDto.class)
        )).thenThrow(
                new UserNotFoundException(999L)
        );

        mockMvc.perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                999L
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(validRequestJson())
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "User with id 999 was not found"
                                )
                );
    }

    @Test
    void shouldReturnConflictWhenCardLimitExceeded()
            throws Exception {

        when(paymentCardService.create(
                eq(USER_ID),
                any(PaymentCardRequestDto.class)
        )).thenThrow(
                new PaymentCardLimitExceededException(USER_ID)
        );

        mockMvc.perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(validRequestJson())
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .exists()
                );
    }

    @Test
    void shouldRejectNonPositiveUserId()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/users/{userId}/payment-cards",
                                0
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );

        verify(paymentCardService, never())
                .findAllByUserId(0L);
    }

    private String validRequestJson() {
        return """
                {
                  "number": "1111222233334444",
                  "holder": "PAVEL KUPREICHIK",
                  "expirationDate": "2030-12-31"
                }
                """;
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