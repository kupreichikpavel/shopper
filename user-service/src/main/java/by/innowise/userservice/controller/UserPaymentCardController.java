package by.innowise.userservice.controller;

import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}/payment-cards")
public class UserPaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    public ResponseEntity<PaymentCardResponseDto> create(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long userId,
            @Valid @RequestBody PaymentCardRequestDto dto
    ) {
        PaymentCardResponseDto createdCard =
                paymentCardService.create(userId, dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCard);
    }

    @GetMapping
    public ResponseEntity<List<PaymentCardResponseDto>> findAllByUserId(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long userId
    ) {
        return ResponseEntity.ok(
                paymentCardService.findAllByUserId(userId)
        );
    }
}