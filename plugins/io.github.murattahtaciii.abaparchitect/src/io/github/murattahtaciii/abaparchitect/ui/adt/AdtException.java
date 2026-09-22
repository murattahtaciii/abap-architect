package io.github.murattahtaciii.abaparchitect.ui.adt;

public class AdtException extends Exception {

    private static final long serialVersionUID = 1L;

    public AdtException(String message) {
        super(message);
    }

    public AdtException(String message, Throwable cause) {
        super(message, cause);
    }
}
