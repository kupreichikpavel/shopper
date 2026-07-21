package by.innowise.userservice.service.impl;

import by.innowise.userservice.dto.paymentcard.PaymentCardCreateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardUpdateDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCardServiceImpl implements PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Override
    @Transactional
    public PaymentCardResponseDto create(
            Long userId,
            PaymentCardCreateDto dto
    ) {
        User user = findUserById(userId);

        long cardsCount =
                paymentCardRepository.countCardsByUserId(userId);

        if (cardsCount >= User.MAX_PAYMENT_CARDS) {
            throw new PaymentCardLimitExceededException(userId);
        }

        PaymentCard paymentCard =
                paymentCardMapper.toEntity(dto);

        user.addPaymentCard(paymentCard);

        PaymentCard savedPaymentCard =
                paymentCardRepository.save(paymentCard);

        log.info(
                "Created payment card with id={} for user with id={}",
                savedPaymentCard.getId(),
                userId
        );

        return paymentCardMapper.toDto(savedPaymentCard);
    }

    @Override
    public PaymentCardResponseDto findById(Long id) {
        PaymentCard paymentCard = findPaymentCardById(id);

        return paymentCardMapper.toDto(paymentCard);
    }

    @Override
    public List<PaymentCardResponseDto> findAllByUserId(
            Long userId
    ) {
        findUserById(userId);

        return paymentCardRepository
                .findAllByUser_Id(userId)
                .stream()
                .map(paymentCardMapper::toDto)
                .toList();
    }

    @Override
    public Page<PaymentCardResponseDto> findAll(
            String ownerName,
            String ownerSurname,
            Pageable pageable
    ) {
        return paymentCardRepository.findAll(
                PaymentCardSpecifications
                        .byOwnerNameAndSurname(
                                ownerName,
                                ownerSurname
                        ),
                pageable
        ).map(paymentCardMapper::toDto);
    }

    @Override
    @Transactional
    public PaymentCardResponseDto update(
            Long id,
            PaymentCardUpdateDto dto
    ) {
        PaymentCard paymentCard =
                findPaymentCardById(id);

        paymentCardMapper.updateEntity(dto, paymentCard);

        PaymentCard updatedPaymentCard =
                paymentCardRepository.save(paymentCard);

        log.info("Updated payment card with id={}", id);

        return paymentCardMapper.toDto(updatedPaymentCard);
    }

    @Override
    @Transactional
    public PaymentCardResponseDto setActive(
            Long id,
            boolean active
    ) {
        int updatedRows =
                paymentCardRepository.updateActiveById(
                        id,
                        active
                );

        if (updatedRows == 0) {
            throw new PaymentCardNotFoundException(id);
        }

        PaymentCard updatedPaymentCard =
                findPaymentCardById(id);

        log.info(
                "Changed active status for payment card with id={} to {}",
                id,
                active
        );

        return paymentCardMapper.toDto(updatedPaymentCard);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PaymentCard paymentCard =
                findPaymentCardById(id);

        paymentCardRepository.delete(paymentCard);

        log.info("Deleted payment card with id={}", id);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(userId)
                );
    }

    private PaymentCard findPaymentCardById(Long id) {
        return paymentCardRepository.findById(id)
                .orElseThrow(
                        () -> new PaymentCardNotFoundException(id)
                );
    }
}