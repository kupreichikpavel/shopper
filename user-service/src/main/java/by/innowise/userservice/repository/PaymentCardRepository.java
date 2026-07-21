package by.innowise.userservice.repository;

import by.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository
        extends JpaRepository<PaymentCard, Long>,
                JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findAllByUser_Id(Long userId);

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM payment_cards
                    WHERE user_id = :userId
                    """,
            nativeQuery = true
    )
    long countCardsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE payment_cards
                    SET active = :active,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """,
            nativeQuery = true
    )
    int updateActiveById(
            @Param("id") Long id,
            @Param("active") boolean active
    );
}
