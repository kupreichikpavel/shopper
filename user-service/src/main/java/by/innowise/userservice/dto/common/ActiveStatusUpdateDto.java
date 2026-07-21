package by.innowise.userservice.dto.common;

import jakarta.validation.constraints.NotNull;

public record ActiveStatusUpdateDto(

        @NotNull(message = "Active status must not be null")
        Boolean active

) {
}