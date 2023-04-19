package org.technologybrewery.habushu;

/**
 * Habushu-specific exception.
 */
public class HabushuException extends RuntimeException {

    private static final long serialVersionUID = 3791347545296314733L;

    /**
     * {@inheritDoc}
     */
    public HabushuException() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public HabushuException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     */
    public HabushuException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public HabushuException(Throwable cause) {
        super(cause);
    }

}
