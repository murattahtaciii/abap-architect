package io.github.murattahtaciii.abaparchitect.core;

public class JsonParseException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public JsonParseException(String message, int line, int column) {
        super(message + " (satır " + line + ", sütun " + column + ")");
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
