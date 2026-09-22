package com.murattahtaci.abaparchitect.ddic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

public final class AbapTypeMapping {

    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$");

    public static final class TypeInfo {

        public final String ddlType;
        public final String domainType;
        public final int length;
        public final int decimals;
        public final String code;

        TypeInfo(String ddlType, String domainType, int length, int decimals, String code) {
            this.ddlType = ddlType;
            this.domainType = domainType;
            this.length = length;
            this.decimals = decimals;
            this.code = code;
        }
    }

    private AbapTypeMapping() {
    }

    public static TypeInfo forText(String text) {
        return forText(text, false);
    }

    public static TypeInfo forText(String text, boolean inferNumbers) {
        if (text == null || text.isEmpty()) {
            return string();
        }
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return bool();
        }
        if (DATE.matcher(text).matches()) {
            return date();
        }
        if (TIME.matcher(text).matches()) {
            return time();
        }
        if (inferNumbers && text.matches("^-?\\d+$")) {
            try {
                long value = Long.parseLong(text);
                return Math.abs(value) > 2_000_000_000L ? packed(16, 0) : integer();
            } catch (NumberFormatException e) {
                return packed(16, 0);
            }
        }
        if (inferNumbers && text.matches("^-?\\d+\\.\\d+$")) {
            return packed(16, 3);
        }
        return string();
    }

    public static TypeInfo forNumber(Number number) {
        if (number instanceof BigDecimal) {
            BigDecimal decimal = ((BigDecimal) number).stripTrailingZeros();
            if (decimal.scale() > 0) {
                return packed(16, 3);
            }
            BigInteger integral = decimal.toBigIntegerExact();
            return integral.abs().compareTo(BigInteger.valueOf(2_000_000_000L)) > 0 ? packed(16, 0) : integer();
        }
        if (number instanceof Long || number instanceof Integer || number instanceof BigInteger) {
            return Math.abs(number.doubleValue()) > 2_000_000_000d ? packed(16, 0) : integer();
        }
        return packed(16, 3);
    }

    public static TypeInfo bool() {
        return new TypeInfo("abap.char(1)", "CHAR", 1, 0, "BOOL");
    }

    public static TypeInfo date() {
        return new TypeInfo("abap.dats", "DATS", 8, 0, "DATS");
    }

    public static TypeInfo time() {
        return new TypeInfo("abap.tims", "TIMS", 6, 0, "TIMS");
    }

    public static TypeInfo integer() {
        return new TypeInfo("abap.int4", "INT4", 10, 0, "INT4");
    }

    public static TypeInfo packed(int length, int decimals) {
        return new TypeInfo("abap.dec(" + length + "," + decimals + ")", "DEC", length, decimals,
                "DEC" + length + "_" + decimals);
    }

    public static TypeInfo string() {
        return new TypeInfo("abap.string(0)", "STRG", 0, 0, "STRING");
    }

    public static final class Choice {

        public final String label;
        public final String baseDdl;
        public final String domainType;
        public final String code;
        public final int defaultLength;
        public final int defaultDecimals;
        public final boolean lengthEditable;
        public final boolean decimalsEditable;

        Choice(String label, String baseDdl, String domainType, String code, int defaultLength,
                int defaultDecimals, boolean lengthEditable, boolean decimalsEditable) {
            this.label = label;
            this.baseDdl = baseDdl;
            this.domainType = domainType;
            this.code = code;
            this.defaultLength = defaultLength;
            this.defaultDecimals = defaultDecimals;
            this.lengthEditable = lengthEditable;
            this.decimalsEditable = decimalsEditable;
        }
    }

    private static final java.util.List<Choice> CHOICES = java.util.List.of(
            new Choice("STRING", "abap.string", "STRG", "STRING", 0, 0, false, false),
            new Choice("CHAR (uzunluk gir)", "abap.char", "CHAR", "CHAR", 10, 0, true, false),
            new Choice("NUMC (uzunluk gir)", "abap.numc", "NUMC", "NUMC", 10, 0, true, false),
            new Choice("CHAR(1) (boolean)", "abap.char", "CHAR", "BOOL", 1, 0, false, false),
            new Choice("CHAR(1)", "abap.char", "CHAR", "CHAR", 1, 0, false, false),
            new Choice("CHAR(4)", "abap.char", "CHAR", "CHAR", 4, 0, false, false),
            new Choice("CHAR(10)", "abap.char", "CHAR", "CHAR", 10, 0, false, false),
            new Choice("CHAR(20)", "abap.char", "CHAR", "CHAR", 20, 0, false, false),
            new Choice("CHAR(40)", "abap.char", "CHAR", "CHAR", 40, 0, false, false),
            new Choice("CHAR(100)", "abap.char", "CHAR", "CHAR", 100, 0, false, false),
            new Choice("CHAR(255)", "abap.char", "CHAR", "CHAR", 255, 0, false, false),
            new Choice("NUMC(4)", "abap.numc", "NUMC", "NUMC", 4, 0, false, false),
            new Choice("NUMC(8)", "abap.numc", "NUMC", "NUMC", 8, 0, false, false),
            new Choice("NUMC(10)", "abap.numc", "NUMC", "NUMC", 10, 0, false, false),
            new Choice("NUMC(12)", "abap.numc", "NUMC", "NUMC", 12, 0, false, false),
            new Choice("INT4 (tam sayı)", "abap.int4", "INT4", "INT4", 10, 0, false, false),
            new Choice("INT8 (büyük tam sayı)", "abap.int8", "INT8", "INT8", 19, 0, false, false),
            new Choice("DEC(15,2)", "abap.dec", "DEC", "DEC", 15, 2, true, true),
            new Choice("DEC(16,3)", "abap.dec", "DEC", "DEC", 16, 3, true, true),
            new Choice("CURR(15,2)", "abap.curr", "CURR", "CURR", 15, 2, true, true),
            new Choice("QUAN(15,3)", "abap.quan", "QUAN", "QUAN", 15, 3, true, true),
            new Choice("DATS (tarih)", "abap.dats", "DATS", "DATS", 8, 0, false, false),
            new Choice("TIMS (saat)", "abap.tims", "TIMS", "TIMS", 6, 0, false, false),
            new Choice("CLNT (client)", "abap.clnt", "CLNT", "CLNT", 3, 0, false, false));

    public static java.util.List<Choice> choices() {
        return CHOICES;
    }

    /** Yeni alanlar için varsayılan: CHAR(255). */
    public static Choice defaultTextChoice() {
        for (Choice choice : CHOICES) {
            if (choice.label.equals("CHAR(255)")) {
                return choice;
            }
        }
        return CHOICES.get(0);
    }

    public static Choice choiceFor(String ddlType) {
        return choiceFor(ddlType, 0, 0);
    }

    public static Choice choiceFor(String ddlType, int length, int decimals) {
        if (ddlType == null) {
            return CHOICES.get(0);
        }
        for (Choice choice : CHOICES) {
            if (apply(choice, choice.defaultLength, choice.defaultDecimals).ddlType.equals(ddlType)) {
                return choice;
            }
        }
        for (Choice choice : CHOICES) {
            if (ddlType.startsWith(choice.baseDdl + "(") || ddlType.equals(choice.baseDdl)) {
                int effectiveLength = length > 0 ? length : choice.defaultLength;
                int effectiveDecimals = decimals > 0 ? decimals : choice.defaultDecimals;
                TypeInfo info = apply(choice, effectiveLength, effectiveDecimals);
                if (info.ddlType.equals(ddlType)) {
                    String label = info.ddlType.replace("abap.", "").toUpperCase(java.util.Locale.ROOT);
                    return new Choice(label, choice.baseDdl, choice.domainType, choice.code, info.length,
                            info.decimals, choice.lengthEditable, choice.decimalsEditable);
                }
            }
        }
        return CHOICES.get(0);
    }

    public static TypeInfo apply(Choice choice, int length, int decimals) {
        int effectiveLength = choice.lengthEditable ? Math.max(1, length) : choice.defaultLength;
        int effectiveDecimals = choice.decimalsEditable ? Math.max(0, decimals) : choice.defaultDecimals;
        boolean isString = choice.baseDdl.equals("abap.string");
        boolean lengthAlways = choice.baseDdl.equals("abap.char") || choice.baseDdl.equals("abap.numc")
                || isString;
        String ddl;
        if (choice.decimalsEditable) {
            ddl = choice.baseDdl + "(" + effectiveLength + "," + effectiveDecimals + ")";
        } else if (choice.lengthEditable || lengthAlways) {
            ddl = choice.baseDdl + "(" + effectiveLength + ")";
        } else {
            ddl = choice.baseDdl;
        }
        String code;
        if (isString) {
            code = "STRING";
        } else if (choice.decimalsEditable) {
            code = choice.code + effectiveLength + "_" + effectiveDecimals;
        } else if (lengthAlways) {
            code = choice.code + effectiveLength;
        } else {
            code = choice.code;
        }
        return new TypeInfo(ddl, choice.domainType, effectiveLength, effectiveDecimals, code);
    }
}
