package Serializer.utils.Errors;


public class DeserializationError extends Exception {
    public DeserializationError(String message) {
        super(message);
    }

    public DeserializationError(String message, Throwable cause) {
        super(message, cause);
    }
}
