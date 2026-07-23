package by.innowise.userservice.dto.user;

import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;

import java.util.List;

public record UserDetailsResponseDto(
        UserResponseDto user,
        List<PaymentCardResponseDto> paymentCards
){
}
