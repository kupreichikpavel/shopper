package by.innowise.userservice.dto.paymentcard;

import java.time.Instant;
import java.time.LocalDate;

public record PaymentCardResponseDto(
        Long id,
        String number,
        String holder,
        LocalDate expirationDate,
        boolean active,
        Long userId,
        Instant createdAt,
        Instant updatedAt
) {
}