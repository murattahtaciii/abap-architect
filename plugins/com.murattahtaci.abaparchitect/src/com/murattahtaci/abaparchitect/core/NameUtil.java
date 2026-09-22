package com.murattahtaci.abaparchitect.core;

public final class NameUtil {

    private NameUtil() {
    }

    public static String toV(String value, boolean snakeCase) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String result = value;
        if (snakeCase) {
            result = result.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
            result = result.replaceAll("([A-Z])([A-Z][a-z])", "$1_$2");
        }
        result = result.replaceAll("[^a-zA-Z0-9_]", "_");
        if (result.matches("^[0-9].*")) {
            result = "F_" + result;
        }
        result = result.toUpperCase(java.util.Locale.ROOT).replaceAll("_+", "_");
        result = result.replaceAll("^_+", "").replaceAll("_+$", "");
        return result;
    }

    public static String makeSingular(String value) {
        String s = value == null ? "" : value.toUpperCase(java.util.Locale.ROOT);
        if (s.endsWith("IES")) {
            return s.substring(0, s.length() - 3) + "Y";
        }
        if (s.endsWith("S") && !s.endsWith("SS")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String stripTrailingUnderscores(String value) {
        return value == null ? "" : value.replaceAll("_+$", "");
    }

    public static String padEnd(String value, int length) {
        if (value.length() >= length) {
            return value;
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(value);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String replaceFirst(String input, String target, String replacement) {
        return input.replaceFirst(java.util.regex.Pattern.quote(target), java.util.regex.Matcher.quoteReplacement(replacement));
    }
}
