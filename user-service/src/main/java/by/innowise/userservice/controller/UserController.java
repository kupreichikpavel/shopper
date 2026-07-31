package by.innowise.userservice.controller;

import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.user.UserDetailsResponseDto;
import by.innowise.userservice.dto.user.UserRequestDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.service.PaymentCardService;
import by.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final PaymentCardService paymentCardService;

    @PostMapping
    public ResponseEntity<UserResponseDto> create(
            @Valid @RequestBody UserRequestDto dto
    ) {
        UserResponseDto createdUser = userService.create(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findById(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long id
    ) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<UserDetailsResponseDto> findDetailsById(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long id
    ) {
        return ResponseEntity.ok(userService.findDetailsById(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<UserResponseDto> users =
                userService.findAll(name, surname, pageable);

        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> update(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long id,
            @Valid @RequestBody UserRequestDto dto
    ) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponseDto> activate(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long id
    ) {
        return ResponseEntity.ok(userService.setActive(id, true));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponseDto> deactivate(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long id
    ) {
        return ResponseEntity.ok(userService.setActive(id, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long id
    ) {
        userService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/payment-cards")
    public ResponseEntity<PaymentCardResponseDto> createPaymentCard(
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

    @GetMapping("/{userId}/payment-cards")
    public ResponseEntity<List<PaymentCardResponseDto>>
    findPaymentCardsByUserId(
            @PathVariable
            @Positive(message = "User id must be positive")
            Long userId
    ) {
        List<PaymentCardResponseDto> paymentCards =
                paymentCardService.findAllByUserId(userId);

        return ResponseEntity.ok(paymentCards);
    }
}