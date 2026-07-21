package by.innowise.userservice.service;

import by.innowise.userservice.dto.paymentcard.PaymentCardCreateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentCardService {

    PaymentCardResponseDto create(
            Long userId,
            PaymentCardCreateDto dto
    );

    PaymentCardResponseDto findById(Long id);

    List<PaymentCardResponseDto> findAllByUserId(Long userId);

    Page<PaymentCardResponseDto> findAll(
            String ownerName,
            String ownerSurname,
            Pageable pageable
    );

    PaymentCardResponseDto update(
            Long id,
            PaymentCardUpdateDto dto
    );

    PaymentCardResponseDto setActive(
            Long id,
            boolean active
    );

    void delete(Long id);
}