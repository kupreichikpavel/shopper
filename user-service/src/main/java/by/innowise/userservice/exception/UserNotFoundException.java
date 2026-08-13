package by.innowise.userservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User with id " + id + " was not found");
    }

    public UserNotFoundException(String email) {
        super("User with email " + email + " was not found");
    }
}
