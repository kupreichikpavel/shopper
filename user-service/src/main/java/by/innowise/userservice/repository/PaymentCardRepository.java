package by.innowise.userservice.repository;

import by.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
}
