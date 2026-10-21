package ek.osnb.demo.coffeeshop.common;

/**
 * The request is well-formed, but not allowed in the current state,
 * e.g. ordering more than is in stock or cancelling a shipped order.
 * Mapped to 409 Conflict by {@link ApiExceptionHandler}.
 *
 * Features extend it with their own exceptions (see InsufficientStockException),
 * so the handler never has to change when a feature is added.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
