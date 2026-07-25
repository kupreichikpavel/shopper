package by.innowise.userservice.exception.handler;

import by.innowise.userservice.exception.UserNotFoundException;
import by.innowise.userservice.exception.PaymentCardLimitExceededException;
import by.innowise.userservice.exception.PaymentCardNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import by.innowise.userservice.exception.EmailAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
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
                .forEach(error ->
                        fieldErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
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
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
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
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return createProblemDetail(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
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
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Request contains invalid data",
                request
        );
    }
}