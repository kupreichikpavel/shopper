package by.innowise.userservice.controller;

import by.innowise.userservice.config.SecurityConfig;
import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.exception.PaymentCardNotFoundException;
import by.innowise.userservice.exception.handler.GlobalExceptionHandler;
import by.innowise.userservice.service.PaymentCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentCardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class
})
class PaymentCardControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentCardService paymentCardService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @SuppressWarnings("unused")
    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void shouldFindPaymentCardById() throws Exception {
        when(paymentCardService.findById(CARD_ID))
                .thenReturn(createResponseDto());

        mockMvc.perform(
                        get(
                                "/api/v1/payment-cards/{id}",
                                CARD_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CARD_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID));

        verify(paymentCardService).findById(CARD_ID);
    }

    @Test
    void shouldFindPaymentCardsWithFiltersAndPagination()
            throws Exception {

        when(paymentCardService.findAll(
                eq("Pav"),
                eq("Kup"),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(
                        List.of(createResponseDto())
                )
        );

        mockMvc.perform(
                        get("/api/v1/payment-cards")
                                .param("ownerName", "Pav")
                                .param("ownerSurname", "Kup")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(CARD_ID)
                )
                .andExpect(
                        jsonPath("$.content[0].userId")
                                .value(USER_ID)
                );

        verify(paymentCardService).findAll(
                eq("Pav"),
                eq("Kup"),
                any(Pageable.class)
        );
    }

    @Test
    void shouldUpdatePaymentCard() throws Exception {
        when(paymentCardService.update(
                eq(CARD_ID),
                any(PaymentCardRequestDto.class)
        )).thenReturn(createResponseDto());

        mockMvc.perform(
                        put(
                                "/api/v1/payment-cards/{id}",
                                CARD_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequestJson())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CARD_ID));

        verify(paymentCardService).update(
                eq(CARD_ID),
                any(PaymentCardRequestDto.class)
        );
    }

    @Test
    void shouldActivatePaymentCard() throws Exception {
        when(paymentCardService.setActive(CARD_ID, true))
                .thenReturn(createResponseDto());

        mockMvc.perform(
                        patch(
                                "/api/v1/payment-cards/{id}/activate",
                                CARD_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        verify(paymentCardService)
                .setActive(CARD_ID, true);
    }

    @Test
    void shouldDeactivatePaymentCard() throws Exception {
        when(paymentCardService.setActive(CARD_ID, false))
                .thenReturn(createInactiveResponseDto());

        mockMvc.perform(
                        patch(
                                "/api/v1/payment-cards/{id}/deactivate",
                                CARD_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(paymentCardService)
                .setActive(CARD_ID, false);
    }

    @Test
    void shouldDeletePaymentCard() throws Exception {
        mockMvc.perform(
                        delete(
                                "/api/v1/payment-cards/{id}",
                                CARD_ID
                        )
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(paymentCardService).delete(CARD_ID);
    }

    @Test
    void shouldReturnNotFoundWhenPaymentCardDoesNotExist()
            throws Exception {

        when(paymentCardService.findById(999L))
                .thenThrow(
                        new PaymentCardNotFoundException(999L)
                );

        mockMvc.perform(
                        get(
                                "/api/v1/payment-cards/{id}",
                                999L
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Payment card with id 999 was not found"
                                )
                );
    }

    @Test
    void shouldRejectNonPositivePaymentCardId()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/payment-cards/{id}",
                                0
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400));

        verify(paymentCardService, never())
                .findById(0L);
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

    private PaymentCardResponseDto createInactiveResponseDto() {
        return new PaymentCardResponseDto(
                CARD_ID,
                "1111222233334444",
                "PAVEL KUPREICHIK",
                LocalDate.of(2030, 12, 31),
                false,
                USER_ID,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z")
        );
    }
}
