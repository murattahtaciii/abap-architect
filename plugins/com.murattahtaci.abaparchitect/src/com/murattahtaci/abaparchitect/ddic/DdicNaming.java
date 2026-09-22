package com.murattahtaci.abaparchitect.ddic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DdicNaming {

    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList(
            "TYPES", "DATA", "TYPE", "TABLE", "FIELD", "KEY", "LINE", "BEGIN", "END",
            "CLASS", "METHOD", "FORM", "FUNCTION", "SELECT", "INSERT", "UPDATE", "DELETE",
            "CREATE", "ALTER", "DROP", "INDEX", "VIEW", "STRUCTURE", "CLIENT", "SYSTEM"));

    private DdicNaming() {
    }

    public static boolean isCustomerNamespace(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.startsWith("Z") || name.startsWith("Y")) {
            return true;
        }
        return name.startsWith("/") && name.indexOf('/', 1) > 1;
    }

    public static String validate(String rawName, DdicKind kind) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "İsim boş olamaz.";
        }
        String name = rawName.trim().toUpperCase(Locale.ROOT);
        int max = kind.maxLength;
        if (name.length() > max) {
            return "İsim çok uzun: " + name.length() + " karakter (en fazla " + max
                    + (kind == DdicKind.TABLE ? ", DDIC tabloları için 16)" : ").");
        }
        if (!name.matches("[A-Z0-9_/]+")) {
            return "Geçersiz karakter. Sadece A-Z, 0-9, _ ve / kullanılabilir.";
        }
        if (!name.startsWith("/") && !name.matches("^[A-Z].*")) {
            return "İsim bir harfle başlamalı.";
        }
        if (name.startsWith("/") && !name.matches("^/[A-Z0-9_]+/[A-Z0-9_]+$")) {
            return "Namespace biçimi /NS/ISIM olmalıdır.";
        }
        if (!isCustomerNamespace(name)) {
            return "Sadece Z... veya Y... (ya da /NAMESPACE/) müşteri isimleri kullanılabilir.";
        }
        if (isTablBased(kind) && name.length() > 2
                && (name.charAt(1) == '_' || name.charAt(2) == '_')) {
            return "Structure/tablo adlarında 2. veya 3. karakter alt tire olamaz (ör. ZFI_S_... geçerli, Z_S_... geçersiz).";
        }
        String base = name.startsWith("/") ? name.substring(name.indexOf('/', 1)) : name;
        if (RESERVED.contains(base)) {
            return "Rezerve ABAP kelimesi kullanılamaz: " + base;
        }
        return null;
    }

    /** Structure ve transparent tablo adları TABL tabanlıdır; erken alt tire kuralı bunlara uygulanır. */
    private static boolean isTablBased(DdicKind kind) {
        return kind == DdicKind.STRUCTURE || kind == DdicKind.TABLE;
    }

    public static String suggestStructure(DdicPlanOptions options, String base) {
        return fixEarlyUnderscores(compose(options.modulePrefix, options.structureCode, base, 30));
    }

    public static String suggestTableType(DdicPlanOptions options, String base) {
        return compose(options.modulePrefix, options.tableTypeCode, base, 30);
    }

    public static String suggestTable(DdicPlanOptions options, String base) {
        return fixEarlyUnderscores(compose(options.modulePrefix, options.tableCode, base, 16));
    }

    public static String suggestDataElement(DdicPlanOptions options, String fieldName) {
        return compose(options.modulePrefix, options.elementCode, fieldName, 30);
    }

    public static String suggestDomain(DdicPlanOptions options, String typeCode) {
        return compose(options.modulePrefix, options.domainCode, typeCode, 30);
    }

    /** İsim düzeni: <MODÜL>_<KOD>_<AD>  (örn. ZFI_T_DTYP, ZSD_DE_MATNR). */
    public static String compose(String modulePrefix, String code, String base, int maxLength) {
        String prefix = sanitize(modulePrefix);
        if (prefix.isEmpty()) {
            prefix = "Z";
        }
        String kindCode = sanitize(code);
        String name = sanitize(base);
        String head = prefix + (kindCode.isEmpty() ? "" : "_" + kindCode);
        if (!name.isEmpty()) {
            head = head + "_";
        }
        String candidate = head + name;
        if (candidate.length() <= maxLength) {
            return trimUnderscores(candidate);
        }
        int room = maxLength - head.length();
        if (room < 1) {
            return candidate.substring(0, maxLength);
        }
        String shortened = name.length() > room ? name.substring(0, room) : name;
        return trimUnderscores(head + trimUnderscores(shortened));
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_")
                .replaceAll("^_+", "").replaceAll("_+$", "").replaceAll("_+", "_");
    }

    /**
     * Structure/tablo adlarında 2. veya 3. karakter alt tire olamaz (SAP DT101).
     * Erken alt tireler ad ile birleştirilir: Z_S_SALES -> ZSSALES.
     */
    static String fixEarlyUnderscores(String name) {
        if (name == null || name.length() <= 2) {
            return name;
        }
        if (name.charAt(1) != '_' && name.charAt(2) != '_') {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((i == 1 || i == 2) && c == '_') {
                continue;
            }
            sb.append(c);
        }
        String fixed = sb.toString();
        if (fixed.length() > 3 && fixed.charAt(2) == '_') {
            fixed = fixed.substring(0, 2) + fixed.substring(3);
        }
        return fixed;
    }

    private static String trimUnderscores(String value) {
        return value.replaceAll("^_+", "").replaceAll("_+$", "");
    }
}
