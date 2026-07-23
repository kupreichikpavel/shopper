package by.innowise.userservice.controller;

import by.innowise.userservice.dto.user.UserCreateDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.dto.user.UserUpdateDto;
import by.innowise.userservice.exception.EmailAlreadyExistsException;
import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.exception.handler.GlobalExceptionHandler;
import by.innowise.userservice.service.UserService;
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

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldCreateUser() throws Exception {
        UserResponseDto responseDto = createResponseDto();

        when(userService.create(any(UserCreateDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "name": "Pavel",
                                          "surname": "Kupreichik",
                                          "birthDate": "2006-01-01",
                                          "email": "pavel@example.com"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        "application/json"
                                )
                )
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.name").value("Pavel"))
                .andExpect(
                        jsonPath("$.surname")
                                .value("Kupreichik")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("pavel@example.com")
                )
                .andExpect(jsonPath("$.active").value(true));

        verify(userService)
                .create(any(UserCreateDto.class));
    }

    @Test
    void shouldFindUserById() throws Exception {
        when(userService.findById(USER_ID))
                .thenReturn(createResponseDto());

        mockMvc.perform(
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
                new PageImpl<>(List.of(createResponseDto()))
        );

        mockMvc.perform(
                        get("/api/v1/users")
                                .param("name", "Pav")
                                .param("surname", "Kup")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(USER_ID))
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
                any(UserUpdateDto.class)
        )).thenReturn(createResponseDto());

        mockMvc.perform(
                        put("/api/v1/users/{id}", USER_ID)
                                .contentType("application/json")
                                .content("""
                                        {
                                          "name": "Pavel",
                                          "surname": "Kupreichik",
                                          "birthDate": "2006-01-01",
                                          "email": "pavel@example.com"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID));

        verify(userService).update(
                eq(USER_ID),
                any(UserUpdateDto.class)
        );
    }

    @Test
    void shouldDeactivateUser() throws Exception {
        UserResponseDto inactiveUser =
                new UserResponseDto(
                        USER_ID,
                        "Pavel",
                        "Kupreichik",
                        LocalDate.of(2006, 1, 1),
                        "pavel@example.com",
                        false,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-02T10:00:00Z")
                );

        when(userService.setActive(USER_ID, false))
                .thenReturn(inactiveUser);

        mockMvc.perform(
                        patch(
                                "/api/v1/users/{id}/active",
                                USER_ID
                        )
                                .contentType("application/json")
                                .content("""
                                        {
                                          "active": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(userService).setActive(USER_ID, false);
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/users/{id}", USER_ID)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).delete(USER_ID);
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType("application/json")
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
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.message")
                                .value("Request validation failed")
                )
                .andExpect(
                        jsonPath("$.fieldErrors.name").exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.surname").exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.birthDate").exists()
                )
                .andExpect(
                        jsonPath("$.fieldErrors.email").exists()
                );

        verify(userService, never())
                .create(any(UserCreateDto.class));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist()
            throws Exception {

        when(userService.findById(999L))
                .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/v1/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "User with id 999 was not found"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/v1/users/999")
                );
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists()
            throws Exception {

        when(userService.create(any(UserCreateDto.class)))
                .thenThrow(
                        new EmailAlreadyExistsException(
                                "pavel@example.com"
                        )
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "name": "Pavel",
                                          "surname": "Kupreichik",
                                          "birthDate": "2006-01-01",
                                          "email": "pavel@example.com"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldRejectNonPositiveUserId() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(userService, never()).findById(0L);
    }

    private UserResponseDto createResponseDto() {
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
}
