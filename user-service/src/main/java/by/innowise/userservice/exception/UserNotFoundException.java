package by.innowise.userservice.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User with id %d was not found".formatted(userId));
    }
}
