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
import by.innowise.userservice.service.PaymentCardService;
import by.innowise.userservice.specification.PaymentCardSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

    private static final String USER_DETAILS_CACHE = "user-details";

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userAccess.isOwner(#userId, authentication))")
    public PaymentCardResponseDto create(Long userId, PaymentCardRequestDto dto) {
        User user = findUserById(userId);
        long cardsCount = paymentCardRepository.countCardsByUserId(userId);
        if (cardsCount >= User.MAX_PAYMENT_CARDS) {
            throw new PaymentCardLimitExceededException(userId);
        }

        PaymentCard paymentCard = paymentCardMapper.toEntity(dto);
        user.addPaymentCard(paymentCard);
        PaymentCard savedPaymentCard = paymentCardRepository.save(paymentCard);
        evictUserDetailsCache(userId);
        log.info("Created payment card with id={} for user with id={}", savedPaymentCard.getId(), userId);
        return paymentCardMapper.toDto(savedPaymentCard);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @paymentCardAccess.isOwner(#id, authentication))")
    public PaymentCardResponseDto findById(Long id) {
        PaymentCard paymentCard = findPaymentCardById(id);
        return paymentCardMapper.toDto(paymentCard);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @userAccess.isOwner(#userId, authentication))")
    public List<PaymentCardResponseDto> findAllByUserId(Long userId) {
        findUserById(userId);
        return paymentCardRepository.findAllByUser_Id(userId).stream().map(paymentCardMapper::toDto).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PaymentCardResponseDto> findAll(String ownerName, String ownerSurname, Pageable pageable) {
        return paymentCardRepository.findAll(PaymentCardSpecifications.byOwnerNameAndSurname(ownerName, ownerSurname), pageable)
                .map(paymentCardMapper::toDto);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @paymentCardAccess.isOwner(#id, authentication))")
    public PaymentCardResponseDto update(Long id, PaymentCardRequestDto dto) {
        PaymentCard paymentCard = findPaymentCardById(id);
        Long userId = paymentCard.getUser().getId();
        paymentCardMapper.updateEntity(dto, paymentCard);
        PaymentCard updatedPaymentCard = paymentCardRepository.save(paymentCard);
        evictUserDetailsCache(userId);
        log.info("Updated payment card with id={}", id);
        return paymentCardMapper.toDto(updatedPaymentCard);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @paymentCardAccess.isOwner(#id, authentication))")
    public PaymentCardResponseDto setActive(Long id, boolean active) {
        int updatedRows = paymentCardRepository.updateActiveById(id, active);
        if (updatedRows == 0) {
            throw new PaymentCardNotFoundException(id);
        }
        PaymentCard updatedPaymentCard = findPaymentCardById(id);
        evictUserDetailsCache(updatedPaymentCard.getUser().getId());
        log.info("Changed active status for payment card with id={} to {}", id, active);
        return paymentCardMapper.toDto(updatedPaymentCard);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @paymentCardAccess.isOwner(#id, authentication))")
    public void delete(Long id) {
        PaymentCard paymentCard = findPaymentCardById(id);
        Long userId = paymentCard.getUser().getId();
        paymentCardRepository.delete(paymentCard);
        evictUserDetailsCache(userId);
        log.info("Deleted payment card with id={}", id);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private PaymentCard findPaymentCardById(Long id) {
        return paymentCardRepository.findById(id).orElseThrow(() -> new PaymentCardNotFoundException(id));
    }

    private void evictUserDetailsCache(Long userId) {
        Cache cache = cacheManager.getCache(USER_DETAILS_CACHE);
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
