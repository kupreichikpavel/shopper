package by.innowise.userservice.controller;

import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardResponseDto;
import by.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-cards")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> findById(
            @PathVariable
            @Positive(message = "Payment card id must be positive")
            Long id
    ) {
        return ResponseEntity.ok(paymentCardService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardResponseDto>> findAll(
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String ownerSurname,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<PaymentCardResponseDto> cards =
                paymentCardService.findAll(ownerName, ownerSurname, pageable);

        return ResponseEntity.ok(cards);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDto> update(
            @PathVariable
            @Positive(message = "Payment card id must be positive")
            Long id,
            @Valid @RequestBody PaymentCardRequestDto dto
    ) {
        return ResponseEntity.ok(paymentCardService.update(id, dto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<PaymentCardResponseDto> activate(
            @PathVariable
            @Positive(message = "Payment card id must be positive")
            Long id
    ) {
        return ResponseEntity.ok(
                paymentCardService.setActive(id, true)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PaymentCardResponseDto> deactivate(
            @PathVariable
            @Positive(message = "Payment card id must be positive")
            Long id
    ) {
        return ResponseEntity.ok(
                paymentCardService.setActive(id, false)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable
            @Positive(message = "Payment card id must be positive")
            Long id
    ) {
        paymentCardService.delete(id);

        return ResponseEntity.noContent().build();
    }
}