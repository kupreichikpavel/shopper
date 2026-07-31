package by.innowise.userservice.service.impl;

import by.innowise.userservice.dto.user.UserDetailsResponseDto;
import by.innowise.userservice.dto.user.UserRequestDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.entity.User;
import by.innowise.userservice.mapper.PaymentCardMapper;
import by.innowise.userservice.mapper.UserMapper;
import by.innowise.userservice.repository.PaymentCardRepository;
import by.innowise.userservice.repository.UserRepository;
import by.innowise.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(UserServiceCachingTest.TestConfig.class)
class UserServiceCachingTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "pavel@example.com";
    private static final String USER_DETAILS_CACHE = "user-details";

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private PaymentCardMapper paymentCardMapper;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(userRepository, userMapper, paymentCardRepository, paymentCardMapper);
        getUserDetailsCache().clear();
    }

    @Test
    void shouldReturnUserDetailsFromCacheOnSecondCall() {
        User user = createUser();
        UserResponseDto responseDto = createResponseDto();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(paymentCardRepository.findAllByUser_Id(USER_ID)).thenReturn(List.of());

        when(userMapper.toDto(user)).thenReturn(responseDto);

        UserDetailsResponseDto firstResult = userService.findDetailsById(USER_ID);

        UserDetailsResponseDto secondResult = userService.findDetailsById(USER_ID);

        assertEquals(firstResult, secondResult);

        assertNotNull(getUserDetailsCache().get(USER_ID));

        verify(userRepository, times(1)).findById(USER_ID);

        verify(paymentCardRepository, times(1)).findAllByUser_Id(USER_ID);

        verify(userMapper, times(1)).toDto(user);
    }

    @Test
    void shouldEvictCacheWhenUserUpdated() {
        User user = createUser();
        UserResponseDto responseDto = createResponseDto();
        UserRequestDto updateDto = createUpdateDto();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(paymentCardRepository.findAllByUser_Id(USER_ID)).thenReturn(List.of());

        when(userMapper.toDto(user)).thenReturn(responseDto);

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(user);

        userService.findDetailsById(USER_ID);

        assertNotNull(getUserDetailsCache().get(USER_ID));

        userService.update(USER_ID, updateDto);

        assertNull(getUserDetailsCache().get(USER_ID));

        verify(userMapper).updateEntity(updateDto, user);

        verify(userRepository).save(user);
    }

    @Test
    void shouldEvictCacheWhenUserDeleted() {
        User user = createUser();
        UserResponseDto responseDto = createResponseDto();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(paymentCardRepository.findAllByUser_Id(USER_ID)).thenReturn(List.of());

        when(userMapper.toDto(user)).thenReturn(responseDto);

        userService.findDetailsById(USER_ID);

        assertNotNull(getUserDetailsCache().get(USER_ID));

        userService.delete(USER_ID);

        assertNull(getUserDetailsCache().get(USER_ID));

        verify(userRepository).delete(user);
    }

    private Cache getUserDetailsCache() {
        return Objects.requireNonNull(cacheManager.getCache(USER_DETAILS_CACHE));
    }

    private UserRequestDto createUpdateDto() {
        return new UserRequestDto("Pavel", "Kupreichik", LocalDate.of(2006, 1, 1), EMAIL);
    }

    private User createUser() {
        User user = new User();

        user.setId(USER_ID);
        user.setName("Pavel");
        user.setSurname("Kupreichik");
        user.setBirthDate(LocalDate.of(2006, 1, 1));
        user.setEmail(EMAIL);
        user.setActive(true);
        user.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));

        return user;
    }

    private UserResponseDto createResponseDto() {
        return new UserResponseDto(USER_ID, "Pavel", "Kupreichik", LocalDate.of(2006, 1, 1), EMAIL, true, Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(USER_DETAILS_CACHE);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        UserMapper userMapper() {
            return mock(UserMapper.class);
        }

        @Bean
        PaymentCardRepository paymentCardRepository() {
            return mock(PaymentCardRepository.class);
        }

        @Bean
        PaymentCardMapper paymentCardMapper() {
            return mock(PaymentCardMapper.class);
        }

        @Bean
        UserService userService(UserRepository userRepository, UserMapper userMapper, PaymentCardRepository paymentCardRepository, PaymentCardMapper paymentCardMapper) {
            return new UserServiceImpl(userRepository, userMapper, paymentCardRepository, paymentCardMapper);
        }
    }
}
