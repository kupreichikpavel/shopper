package by.innowise.userservice.exception.handler;

import by.innowise.userservice.exception.EmailAlreadyExistsException;
import by.innowise.userservice.exception.PaymentCardLimitExceededException;
import by.innowise.userservice.exception.PaymentCardNotFoundException;
import by.innowise.userservice.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("User not found: {}", exception.getMessage());

        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(PaymentCardNotFoundException.class)
    public ProblemDetail handlePaymentCardNotFound(
            PaymentCardNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("Payment card not found: {}", exception.getMessage());

        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(PaymentCardLimitExceededException.class)
    public ProblemDetail handlePaymentCardLimitExceeded(
            PaymentCardLimitExceededException exception,
            HttpServletRequest request
    ) {
        log.warn("Payment card limit exceeded: {}", exception.getMessage());

        return createProblemDetail(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        log.warn("Email conflict: {}", exception.getMessage());

        return createProblemDetail(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        log.warn(
                "Request validation failed for {} {}. Invalid fields: {}",
                request.getMethod(),
                request.getRequestURI(),
                fieldErrors.keySet()
        );

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request
        );

        problemDetail.setProperty("fieldErrors", fieldErrors);

        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation: {}", exception.getMessage());

        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Data integrity violation while processing {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        return createProblemDetail(
                HttpStatus.CONFLICT,
                "Request conflicts with existing data",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Invalid value for request parameter '{}': {}",
                exception.getName(),
                exception.getValue()
        );

        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Request contains invalid data",
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Unreadable request body for {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Request contains invalid data",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error while processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected internal server error",
                request
        );
    }

    private ProblemDetail createProblemDetail(
            HttpStatus status,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
