package by.innowise.userservice.specification;

import by.innowise.userservice.entity.PaymentCard;
import by.innowise.userservice.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PaymentCardSpecifications {

    private PaymentCardSpecifications() {
    }

    public static Specification<PaymentCard> byOwnerNameAndSurname(
            String name,
            String surname
    ) {
        return (root, query, criteriaBuilder) -> {
            boolean hasName = StringUtils.hasText(name);
            boolean hasSurname = StringUtils.hasText(surname);

            if (!hasName && !hasSurname) {
                return criteriaBuilder.conjunction();
            }

            Join<PaymentCard, User> userJoin =
                    root.join("user", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();

            if (hasName) {
                String normalizedName =
                        name.trim().toLowerCase(Locale.ROOT);

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        userJoin.get("name")
                                ),
                                "%" + normalizedName + "%"
                        )
                );
            }

            if (hasSurname) {
                String normalizedSurname =
                        surname.trim().toLowerCase(Locale.ROOT);

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        userJoin.get("surname")
                                ),
                                "%" + normalizedSurname + "%"
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }
}
