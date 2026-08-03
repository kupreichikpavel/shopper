package by.innowise.userservice.controller;

import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.user.UserRequestDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.exception.EmailAlreadyExistsException;
import by.innowise.userservice.exception.PaymentCardLimitExceededException;
import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.exception.handler.GlobalExceptionHandler;
import by.innowise.userservice.config.SecurityConfig;
import by.innowise.userservice.security.PaymentCardAccess;
import by.innowise.userservice.security.UserAccess;
import by.innowise.userservice.security.ResourceAuthorizationManager;
import by.innowise.userservice.service.PaymentCardService;
import by.innowise.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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

@WebMvcTest(UserController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        ResourceAuthorizationManager.class
})
class UserControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PaymentCardService paymentCardService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserAccess userAccess;

    @MockitoBean
    private PaymentCardAccess paymentCardAccess;

    @SuppressWarnings("unused")
    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void shouldCreateUser() throws Exception {
        when(userService.create(any(UserRequestDto.class)))
                .thenReturn(createUserResponseDto());

        perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserRequestJson())
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.name").value("Pavel"))
                .andExpect(jsonPath("$.surname").value("Kupreichik"))
                .andExpect(jsonPath("$.email").value("pavel@example.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).create(any(UserRequestDto.class));
    }

    @Test
    void shouldFindUserById() throws Exception {
        when(userService.findById(USER_ID))
                .thenReturn(createUserResponseDto());

        perform(
                        get("/api/v1/users/{id}", USER_ID)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.name").value("Pavel"));

        verify(userService).findById(USER_ID);
    }

    @Test
    void shouldFindUsersWithFiltersAndPagination()
            throws Exception {

        when(userService.findAll(
                eq("Pav"),
                eq("Kup"),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(
                        List.of(createUserResponseDto())
                )
        );

        perform(
                        get("/api/v1/users")
                                .param("name", "Pav")
                                .param("surname", "Kup")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(USER_ID)
                )
                .andExpect(
                        jsonPath("$.content[0].name")
                                .value("Pavel")
                );

        verify(userService).findAll(
                eq("Pav"),
                eq("Kup"),
                any(Pageable.class)
        );
    }

    @Test
    void shouldUpdateUser() throws Exception {
        when(userService.update(
                eq(USER_ID),
                any(UserRequestDto.class)
        )).thenReturn(createUserResponseDto());

        perform(
                        put("/api/v1/users/{id}", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserRequestJson())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID));

        verify(userService).update(
                eq(USER_ID),
                any(UserRequestDto.class)
        );
    }

    @Test
    void shouldActivateUser() throws Exception {
        when(userService.setActive(USER_ID, true))
                .thenReturn(createUserResponseDto());

        perform(
                        patch(
                                "/api/v1/users/{id}/activate",
                                USER_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).setActive(USER_ID, true);
    }

    @Test
    void shouldDeactivateUser() throws Exception {
        when(userService.setActive(USER_ID, false))
                .thenReturn(createInactiveUserResponseDto());

        perform(
                        patch(
                                "/api/v1/users/{id}/deactivate",
                                USER_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(userService).setActive(USER_ID, false);
    }

    @Test
    void shouldDeleteUser() throws Exception {
        perform(
                        delete("/api/v1/users/{id}", USER_ID)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).delete(USER_ID);
    }

    @Test
    void shouldRejectInvalidUserCreateRequest()
            throws Exception {

        perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "",
                                          "surname": " ",
                                          "birthDate": "2035-01-01",
                                          "email": "incorrect-email"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Request validation failed")
                )
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.surname").exists())
                .andExpect(jsonPath("$.fieldErrors.birthDate").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verify(userService, never())
                .create(any(UserRequestDto.class));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist()
            throws Exception {

        when(userService.findById(999L))
                .thenThrow(new UserNotFoundException(999L));

        perform(
                        get("/api/v1/users/{id}", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "User with id 999 was not found"
                                )
                );
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists()
            throws Exception {

        when(userService.create(any(UserRequestDto.class)))
                .thenThrow(
                        new EmailAlreadyExistsException(
                                "pavel@example.com"
                        )
                );

        perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserRequestJson())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldRejectNonPositiveUserId()
            throws Exception {

        perform(
                        get("/api/v1/users/{id}", 0)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400));

        verify(userService, never()).findById(0L);
    }

    @Test
    void shouldCreatePaymentCard() throws Exception {
        when(paymentCardService.create(
                eq(USER_ID),
                any(PaymentCardRequestDto.class)
        )).thenReturn(createPaymentCardResponseDto());

        perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validPaymentCardRequestJson())
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
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value(USER_ID));

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
                        List.of(createPaymentCardResponseDto())
                );

        perform(
                        get(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CARD_ID))
                .andExpect(
                        jsonPath("$[0].number")
                                .value("1111222233334444")
                )
                .andExpect(
                        jsonPath("$[0].holder")
                                .value("PAVEL KUPREICHIK")
                )
                .andExpect(jsonPath("$[0].userId").value(USER_ID));

        verify(paymentCardService)
                .findAllByUserId(USER_ID);
    }

    @Test
    void shouldRejectInvalidPaymentCardCreateRequest()
            throws Exception {

        perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "number": "123",
                                          "holder": "",
                                          "expirationDate": "2020-01-01"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Request validation failed")
                )
                .andExpect(jsonPath("$.fieldErrors.number").exists())
                .andExpect(jsonPath("$.fieldErrors.holder").exists())
                .andExpect(
                        jsonPath("$.fieldErrors.expirationDate")
                                .exists()
                );

        verify(paymentCardService, never()).create(
                eq(USER_ID),
                any(PaymentCardRequestDto.class)
        );
    }

    @Test
    void shouldRejectNonPositiveUserIdForPaymentCards()
            throws Exception {

        perform(
                        get(
                                "/api/v1/users/{userId}/payment-cards",
                                0
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400));

        verify(paymentCardService, never())
                .findAllByUserId(0L);
    }

    @Test
    void shouldReturnNotFoundWhenCreatingCardForMissingUser()
            throws Exception {

        when(paymentCardService.create(
                eq(USER_ID),
                any(PaymentCardRequestDto.class)
        )).thenThrow(new UserNotFoundException(USER_ID));

        perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validPaymentCardRequestJson())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "User with id 1 was not found"
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

        perform(
                        post(
                                "/api/v1/users/{userId}/payment-cards",
                                USER_ID
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validPaymentCardRequestJson())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());
    }

    private String validUserRequestJson() {
        return """
                {
                  "name": "Pavel",
                  "surname": "Kupreichik",
                  "birthDate": "2006-01-01",
                  "email": "pavel@example.com"
                }
                """;
    }

    private String validPaymentCardRequestJson() {
        return """
                {
                  "number": "1111222233334444",
                  "holder": "PAVEL KUPREICHIK",
                  "expirationDate": "2030-12-31"
                }
                """;
    }

    private UserResponseDto createUserResponseDto() {
        return new UserResponseDto(
                USER_ID,
                "Pavel",
                "Kupreichik",
                LocalDate.of(2006, 1, 1),
                "pavel@example.com",
                true,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z")
        );
    }

    private UserResponseDto createInactiveUserResponseDto() {
        return new UserResponseDto(
                USER_ID,
                "Pavel",
                "Kupreichik",
                LocalDate.of(2006, 1, 1),
                "pavel@example.com",
                false,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z")
        );
    }

    private PaymentCardResponseDto createPaymentCardResponseDto() {
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

    private ResultActions perform(
            MockHttpServletRequestBuilder request
    ) throws Exception {
        return mockMvc.perform(
                request.with(
                        jwt()
                                .jwt(builder ->
                                        builder.claim(
                                                "userId",
                                                USER_ID
                                        )
                                )
                                .authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMIN"
                                        ),
                                        new SimpleGrantedAuthority(
                                                "ROLE_SERVICE"
                                        )
                                )
                )
        );
    }

}
