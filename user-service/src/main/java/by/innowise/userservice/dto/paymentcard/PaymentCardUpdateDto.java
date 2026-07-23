package by.innowise.userservice.dto.paymentcard;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentCardUpdateDto(
        @NotBlank(message = "Card number must not be blank")
        @Pattern(regexp = "\\d{16,19}", message = "Card number must contain from 16 to 19 digits")
        String number,
        @NotBlank(message = "Card holder must not be blank")
        @Size(max = 255, message = "Card holder must not exceed 255 characters")
        String holder,
        @NotNull(message = "Expiration date must not be null")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {
}
