package utils;

/**
 * Custom exception to handle cases where a user is not found during login
 * or other user-related operations.
 */
public class UserNotFoundException extends Exception {

    /**
     * Constructor to create a UserNotFoundException with a custom message.
     *
     * @param message The custom message for the exception.
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
