package by.innowise.userservice.specification;

import by.innowise.userservice.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> byNameAndSurname(String name, String surname) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(name)) {
                String normalizedName = name.trim().toLowerCase(Locale.ROOT);

                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + normalizedName + "%"));
            }
            if (StringUtils.hasText(surname)) {
                String normalizedSurname = surname.trim().toLowerCase(Locale.ROOT);

                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("surname")), "%" + normalizedSurname + "%"));
            }
            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
