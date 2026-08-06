package by.innowise.userservice.service.impl;

import by.innowise.userservice.dto.user.UserRequestDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.entity.User;
import by.innowise.userservice.exception.EmailAlreadyExistsException;
import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.mapper.UserMapper;
import by.innowise.userservice.repository.UserRepository;
import by.innowise.userservice.service.UserService;
import by.innowise.userservice.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.user.UserDetailsResponseDto;
import by.innowise.userservice.mapper.PaymentCardMapper;
import by.innowise.userservice.repository.PaymentCardRepository;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USER_DETAILS_CACHE = "user-details";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaymentCardRepository paymentCardRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Override
    @Transactional
    public UserResponseDto create(UserRequestDto dto) {
        ensureEmailAvailable(dto.email(), null);
        User user = userMapper.toEntity(dto);
        User savedUser = userRepository.save(user);
        log.info("Created user with id={}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserResponseDto findById(Long id) {
        User user = findUserById(id);
        return userMapper.toDto(user);
    }

    @Override
    public Page<UserResponseDto> findAll(String name, String surname, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.byNameAndSurname(name, surname), pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findByEmail(String email) {
        String normalizedEmail = email.trim();

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new UserNotFoundException(normalizedEmail)
                );

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = USER_DETAILS_CACHE, key = "#id")
    public UserDetailsResponseDto findDetailsById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        List<PaymentCardResponseDto> paymentCards = paymentCardRepository.findAllByUser_Id(id)
                .stream().map(paymentCardMapper::toDto).toList();
        return new UserDetailsResponseDto(userMapper.toDto(user), paymentCards);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_DETAILS_CACHE, key = "#id")
    public UserResponseDto update(Long id, UserRequestDto dto) {
        User user = findUserById(id);
        ensureEmailAvailable(dto.email(), id);
        userMapper.updateEntity(dto, user);
        User updatedUser = userRepository.save(user);
        log.info("Updated user with id={}", id);
        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_DETAILS_CACHE, key = "#id")
    public UserResponseDto setActive(Long id, boolean active) {
        int updatedRows = userRepository.updateActiveById(id, active);
        if (updatedRows == 0) {
            throw new UserNotFoundException(id);
        }
        User updatedUser = findUserById(id);
        log.info("Changed active status for user with id={} to {}", id, active);
        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = USER_DETAILS_CACHE, key = "#id")
    public void delete(Long id) {
        User user = findUserById(id);
        userRepository.delete(user);
        log.info("Deleted user with id={}", id);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        userRepository.findByEmailIgnoreCase(email).filter(existingUser -> !Objects.equals(existingUser.getId(), currentUserId)).ifPresent(existingUser -> {
            throw new EmailAlreadyExistsException(email);
        });
    }
}
