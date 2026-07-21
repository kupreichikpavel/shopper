package by.innowise.userservice.service.impl;

import by.innowise.userservice.dto.user.UserCreateDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.dto.user.UserUpdateDto;
import by.innowise.userservice.entity.User;
import by.innowise.userservice.exception.EmailAlreadyExistsException;
import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.mapper.UserMapper;
import by.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "pavel@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldCreateUser() {
        UserCreateDto createDto = createUserDto();
        User user = createUser();
        UserResponseDto responseDto = createResponseDto();

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.empty());

        when(userMapper.toEntity(createDto))
                .thenReturn(user);

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toDto(user))
                .thenReturn(responseDto);

        UserResponseDto result = userService.create(createDto);

        assertSame(responseDto, result);

        verify(userRepository)
                .findByEmailIgnoreCase(EMAIL);

        verify(userMapper)
                .toEntity(createDto);

        verify(userRepository)
                .save(user);

        verify(userMapper)
                .toDto(user);
    }

    @Test
    void shouldRejectUserWithExistingEmail() {
        UserCreateDto createDto = createUserDto();
        User existingUser = createUser();

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.create(createDto)
        );

        verifyNoInteractions(userMapper);

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldFindUserById() {
        User user = createUser();
        UserResponseDto responseDto = createResponseDto();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userMapper.toDto(user))
                .thenReturn(responseDto);

        UserResponseDto result =
                userService.findById(USER_ID);

        assertSame(responseDto, result);

        verify(userRepository).findById(USER_ID);
        verify(userMapper).toDto(user);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.findById(USER_ID)
        );

        verifyNoInteractions(userMapper);
    }

    @Test
    void shouldFindUsersWithFilteringAndPagination() {
        User user = createUser();
        UserResponseDto responseDto = createResponseDto();

        Pageable pageable = PageRequest.of(0, 10);

        Page<User> users =
                new PageImpl<>(
                        List.of(user),
                        pageable,
                        1
                );

        when(userRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(users);

        when(userMapper.toDto(user))
                .thenReturn(responseDto);

        Page<UserResponseDto> result =
                userService.findAll(
                        "Pav",
                        "Kup",
                        pageable
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertSame(
                responseDto,
                result.getContent().getFirst()
        );

        verify(userRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    void shouldUpdateUser() {
        UserUpdateDto updateDto = createUpdateDto();
        User user = createUser();
        UserResponseDto responseDto = createResponseDto();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toDto(user))
                .thenReturn(responseDto);

        UserResponseDto result =
                userService.update(USER_ID, updateDto);

        assertSame(responseDto, result);

        verify(userMapper).updateEntity(updateDto, user);
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectEmailBelongingToAnotherUser() {
        UserUpdateDto updateDto = createUpdateDto();

        User currentUser = createUser();
        currentUser.setId(USER_ID);

        User anotherUser = createUser();
        anotherUser.setId(2L);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(currentUser));

        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(anotherUser));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.update(
                        USER_ID,
                        updateDto
                )
        );

        verify(userMapper, never())
                .updateEntity(any(), any());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldChangeUserActiveStatus() {
        User user = createUser();
        user.setActive(false);

        UserResponseDto responseDto = new UserResponseDto(
                USER_ID,
                user.getName(),
                user.getSurname(),
                user.getBirthDate(),
                user.getEmail(),
                false,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        when(userRepository.updateActiveById(
                USER_ID,
                false
        )).thenReturn(1);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userMapper.toDto(user))
                .thenReturn(responseDto);

        UserResponseDto result =
                userService.setActive(USER_ID, false);

        assertEquals(false, result.active());

        verify(userRepository)
                .updateActiveById(USER_ID, false);
    }

    @Test
    void shouldThrowExceptionWhenChangingMissingUser() {
        when(userRepository.updateActiveById(
                USER_ID,
                false
        )).thenReturn(0);

        assertThrows(
                UserNotFoundException.class,
                () -> userService.setActive(
                        USER_ID,
                        false
                )
        );

        verify(userRepository, never())
                .findById(USER_ID);
    }

    @Test
    void shouldDeleteUser() {
        User user = createUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        userService.delete(USER_ID);

        verify(userRepository).findById(USER_ID);
        verify(userRepository).delete(user);
    }

    private UserCreateDto createUserDto() {
        return new UserCreateDto(
                "Pavel",
                "Kupreichik",
                LocalDate.of(2006, 1, 1),
                EMAIL
        );
    }

    private UserUpdateDto createUpdateDto() {
        return new UserUpdateDto(
                "Pavel",
                "Kupreichik",
                LocalDate.of(2006, 1, 1),
                EMAIL
        );
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setName("Pavel");
        user.setSurname("Kupreichik");
        user.setBirthDate(
                LocalDate.of(2006, 1, 1)
        );
        user.setEmail(EMAIL);
        user.setActive(true);
        user.setCreatedAt(
                Instant.parse("2026-01-01T10:00:00Z")
        );
        user.setUpdatedAt(
                Instant.parse("2026-01-01T10:00:00Z")
        );
        return user;
    }

    private UserResponseDto createResponseDto() {
        return new UserResponseDto(
                USER_ID,
                "Pavel",
                "Kupreichik",
                LocalDate.of(2006, 1, 1),
                EMAIL,
                true,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }
}