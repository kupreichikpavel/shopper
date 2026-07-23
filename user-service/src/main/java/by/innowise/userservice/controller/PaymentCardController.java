package by.innowise.userservice.controller;

import by.innowise.userservice.dto.common.ActiveStatusUpdateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardCreateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardUpdateDto;
import by.innowise.userservice.service.PaymentCardService;
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
@RequestMapping("/api/v1")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping("/users/{userId}/payment-cards")
    public ResponseEntity<PaymentCardResponseDto> create(@PathVariable @Positive(message = "User id must be positive") Long userId,
                                                         @Valid @RequestBody PaymentCardCreateDto dto) {
        PaymentCardResponseDto createdCard = paymentCardService.create(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
    }

    @GetMapping("/payment-cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> findById(@PathVariable @Positive(message = "Payment card id must be positive") Long id) {
        return ResponseEntity.ok(paymentCardService.findById(id));
    }

    @GetMapping("/users/{userId}/payment-cards")
    public ResponseEntity<List<PaymentCardResponseDto>> findAllByUserId(@PathVariable @Positive(message = "User id must be positive") Long userId) {
        return ResponseEntity.ok(paymentCardService.findAllByUserId(userId));
    }

    @GetMapping("/payment-cards")
    public ResponseEntity<Page<PaymentCardResponseDto>> findAll(@RequestParam(required = false) String ownerName,
                                                                @RequestParam(required = false) String ownerSurname,
                                                                @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<PaymentCardResponseDto> cards = paymentCardService.findAll(ownerName, ownerSurname, pageable);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/payment-cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> update(@PathVariable @Positive(message = "Payment card id must be positive") Long id,
                                                         @Valid @RequestBody PaymentCardUpdateDto dto) {
        return ResponseEntity.ok(paymentCardService.update(id, dto));
    }

    @PatchMapping("/payment-cards/{id}/active")
    public ResponseEntity<PaymentCardResponseDto> setActive(@PathVariable @Positive(message = "Payment card id must be positive") Long id,
                                                            @Valid @RequestBody ActiveStatusUpdateDto dto) {
        return ResponseEntity.ok(paymentCardService.setActive(id, dto.active()));
    }

    @DeleteMapping("/payment-cards/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive(message = "Payment card id must be positive") Long id) {
        paymentCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
