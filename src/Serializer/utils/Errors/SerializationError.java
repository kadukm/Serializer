package Serializer.utils.Errors;


public class SerializationError extends Exception {
    public SerializationError(String message) {
        super(message);
    }

    public SerializationError(String message, Throwable cause) {
        super(message, cause);
    }
}
