package by.innowise.userservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    public static final int MAX_PAYMENT_CARDS = 5;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "surname", nullable = false, length = 100)
    private String surname;
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentCard> paymentCards = new ArrayList<>();

    public void addPaymentCard(PaymentCard paymentCard) {
        Objects.requireNonNull(paymentCard, "Payment card must not be null");
        if (paymentCards.size() >= MAX_PAYMENT_CARDS) {
            throw new IllegalStateException("User cannot have more than 5 payment cards");
        }
        paymentCards.add(paymentCard);
        paymentCard.setUser(this);
    }

    public void removePaymentCard(PaymentCard paymentCard) {
        Objects.requireNonNull(paymentCard, "Payment card must not be null");
        if (paymentCards.remove(paymentCard)) {
            paymentCard.setUser(null);
        }
    }
}
