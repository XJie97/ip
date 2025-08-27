package tkit;

/** Checked exception used for input and command errors in Tkit. */
class TkitException extends Exception {
    /** Creates an exception carrying a user-facing message. */
    public TkitException(String message) {
        super(message);
    }
}
