package by.innowise.userservice.dto;

import by.innowise.userservice.dto.paymentcard.PaymentCardRequestDto;
import by.innowise.userservice.dto.user.UserRequestDto;
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
        validatorFactory = Validation.buildDefaultValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldRejectInvalidUserRequestDtoCreated() {
        UserRequestDto dto = new UserRequestDto(" ", "", LocalDate.now().plusDays(1), "invalid-email");

        Set<String> invalidFields = getInvalidFields(dto);

        assertTrue(invalidFields.contains("name"));
        assertTrue(invalidFields.contains("surname"));
        assertTrue(invalidFields.contains("birthDate"));
        assertTrue(invalidFields.contains("email"));
    }

    @Test
    void shouldAcceptValidUserRequestDtoUpdate() {
        UserRequestDto dto = new UserRequestDto("Pavel", "Kupreichik", LocalDate.of(2006, 1, 1), "pavel@example.com");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidUserRequestDto() {
        UserRequestDto dto = new UserRequestDto("", " ", LocalDate.now().plusYears(1), "wrong");

        Set<String> invalidFields = getInvalidFields(dto);

        assertEquals(Set.of("name", "surname", "birthDate", "email"), invalidFields);
    }

    @Test
    void shouldRejectInvalidPaymentCardRequestDtoCreated() {
        PaymentCardRequestDto dto = new PaymentCardRequestDto("123-456", " ", LocalDate.now().minusDays(1));
        Set<String> invalidFields = getInvalidFields(dto);

        assertTrue(invalidFields.contains("number"));
        assertTrue(invalidFields.contains("holder"));
        assertTrue(invalidFields.contains("expirationDate"));
    }

    @Test
    void shouldAcceptValidPaymentCardRequestDto() {
        PaymentCardRequestDto dto = new PaymentCardRequestDto("1111222233334444", "PAVEL KUPREICHIK", LocalDate.now().plusYears(3));

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void shouldRejectInvalidPaymentCardRequestDtoUpdate() {
        PaymentCardRequestDto dto = new PaymentCardRequestDto("123", "", LocalDate.now());
        Set<String> invalidFields = getInvalidFields(dto);

        assertEquals(Set.of("number", "holder", "expirationDate"), invalidFields);
    }

    private Set<String> getInvalidFields(Object dto) {
        return validator.validate(dto)
                .stream().
                map(ConstraintViolation::getPropertyPath)
                .map(Object::toString).collect(Collectors.toSet());
    }
}