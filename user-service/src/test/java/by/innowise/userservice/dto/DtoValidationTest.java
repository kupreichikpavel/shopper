package by.innowise.userservice.dto;

import by.innowise.userservice.dto.paymentcard.PaymentCardCreateDto;
import by.innowise.userservice.dto.paymentcard.PaymentCardUpdateDto;
import by.innowise.userservice.dto.user.UserCreateDto;
import by.innowise.userservice.dto.user.UserUpdateDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory =
                Validation.buildDefaultValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldRejectInvalidUserCreateDto() {
        UserCreateDto dto = new UserCreateDto(
                " ",
                "",
                LocalDate.now().plusDays(1),
                "invalid-email"
        );

        Set<String> invalidFields =
                getInvalidFields(dto);

        assertTrue(invalidFields.contains("name"));
        assertTrue(invalidFields.contains("surname"));
        assertTrue(invalidFields.contains("birthDate"));
        assertTrue(invalidFields.contains("email"));
    }

    @Test
    void shouldAcceptValidUserCreateDto() {
        UserCreateDto dto = new UserCreateDto(
                "Pavel",
                "Kupreichik",
                LocalDate.of(2006, 1, 1),
                "pavel@example.com"
        );

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidUserUpdateDto() {
        UserUpdateDto dto = new UserUpdateDto(
                "",
                " ",
                LocalDate.now().plusYears(1),
                "wrong"
        );

        Set<String> invalidFields =
                getInvalidFields(dto);

        assertEquals(
                Set.of(
                        "name",
                        "surname",
                        "birthDate",
                        "email"
                ),
                invalidFields
        );
    }

    @Test
    void shouldRejectInvalidPaymentCardCreateDto() {
        PaymentCardCreateDto dto =
                new PaymentCardCreateDto(
                        "123-456",
                        " ",
                        LocalDate.now().minusDays(1)
                );

        Set<String> invalidFields =
                getInvalidFields(dto);

        assertTrue(invalidFields.contains("number"));
        assertTrue(invalidFields.contains("holder"));
        assertTrue(
                invalidFields.contains("expirationDate")
        );
    }

    @Test
    void shouldAcceptValidPaymentCardCreateDto() {
        PaymentCardCreateDto dto =
                new PaymentCardCreateDto(
                        "1111222233334444",
                        "PAVEL KUPREICHIK",
                        LocalDate.now().plusYears(3)
                );

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidPaymentCardUpdateDto() {
        PaymentCardUpdateDto dto =
                new PaymentCardUpdateDto(
                        "123",
                        "",
                        LocalDate.now()
                );

        Set<String> invalidFields =
                getInvalidFields(dto);

        assertEquals(
                Set.of(
                        "number",
                        "holder",
                        "expirationDate"
                ),
                invalidFields
        );
    }

    private Set<String> getInvalidFields(Object dto) {
        return validator.validate(dto)
                .stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}