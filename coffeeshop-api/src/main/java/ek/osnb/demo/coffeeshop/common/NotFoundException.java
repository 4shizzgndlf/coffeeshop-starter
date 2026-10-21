package ek.osnb.demo.coffeeshop.common;

/**
 * Something the client asked for does not exist, e.g. an unknown product id.
 * Mapped to 404 Not Found by {@link ApiExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
