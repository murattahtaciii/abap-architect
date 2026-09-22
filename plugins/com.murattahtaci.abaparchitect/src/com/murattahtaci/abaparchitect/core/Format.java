package com.murattahtaci.abaparchitect.core;

public enum Format {

    JSON,
    XML;

    public static Format detect(String text) {
        if (text == null) {
            return JSON;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.startsWith("<?xml") || (trimmed.startsWith("<") && !trimmed.startsWith("{"))) {
            return XML;
        }
        return JSON;
    }
}
