package by.innowise.userservice.dto.user;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserCreateDto(
        @NotBlank @Size(max = 255)
        String name,
        @NotBlank @Size(max = 255)
        String surname,
        @NotNull @Past
        LocalDate birthDate,
        @NotBlank @Email @Size(max = 255)
        String email
) {
}
