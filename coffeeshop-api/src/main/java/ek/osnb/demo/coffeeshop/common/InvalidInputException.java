package ek.osnb.demo.coffeeshop.common;

/**
 * A value breaks a rule of the domain, e.g. a quantity of 0.
 * Thrown by entities that guard their own rules, so they stay valid even when
 * they are used without a controller (in tests, or from another service).
 * Mapped to 400 Bad Request by {@link ApiExceptionHandler}.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
