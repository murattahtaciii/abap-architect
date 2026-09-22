package io.github.murattahtaciii.abaparchitect.ddic;

import java.util.List;
import java.util.Locale;

public final class DdicDdlBuilder {

    /** DDIC DDL kaynağında bileşen adı olarak kullanılamayacak kelimeler. */
    private static final java.util.Set<String> RESERVED = java.util.Set.of(
            "key", "include", "client", "not", "null", "define", "structure", "table",
            "extend", "extendtype", "abstract", "final", "with", "last", "provider",
            "source", "element", "of", "as", "and", "or", "case", "select", "view",
            "append", "constant", "decimals", "length", "annotations", "type", "types");

    private DdicDdlBuilder() {
    }

    public static String structure(String name, String description, List<DdicModel.Field> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("@EndUserText.label : '").append(escape(description)).append("'\n");
        sb.append("@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE\n");
        sb.append("define structure ").append(name.toLowerCase(Locale.ROOT)).append(" {\n");
        int count = appendFields(sb, fields, 30, true, false);
        if (count == 0) {
            sb.append("  dummy_field : abap.char(1);\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    public static String table(String name, String description, List<DdicModel.Field> fields,
            boolean addClientField) {
        StringBuilder sb = new StringBuilder();
        sb.append("@EndUserText.label : '").append(escape(description)).append("'\n");
        sb.append("@AbapCatalog.enhancementCategory : #NOT_EXTENSIBLE\n");
        sb.append("@AbapCatalog.tableCategory : #TRANSPARENT\n");
        sb.append("@AbapCatalog.deliveryClass : #A\n");
        sb.append("@AbapCatalog.dataMaintenance : #RESTRICTED\n");
        sb.append("define table ").append(name.toLowerCase(Locale.ROOT)).append(" {\n");
        boolean clientEmitted = false;
        if (addClientField) {
            sb.append("  key client : abap.clnt not null;\n");
            clientEmitted = true;
        }
        int count = appendFields(sb, fields, 16, true, true);
        if (count == 0 && !clientEmitted) {
            sb.append("  key dummy_field : abap.char(1) not null;\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Alan listesini yazar; include edilen iç içe yapılar yerinde (inline)
     * yazılır. Tablolarda anahtar alanlar önce, sonrasında diğerleri yazılır
     * (anahtar alanlar başta kesintisiz olmak zorundadır); STRING tipler
     * anahtar yapılamaz. DDL parser'i yorum satirlarini desteklemedigi icin
     * hicbir aciklama uretilmez; donus degeri yazilan bilesen sayisidir.
     */
    private static int appendFields(StringBuilder sb, List<DdicModel.Field> fields, int maxNameLength,
            boolean expandIncludes, boolean keysFirst) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<DdicModel.Field> keys = new java.util.ArrayList<>();
        List<DdicModel.Field> rest = new java.util.ArrayList<>();
        if (fields != null) {
            for (DdicModel.Field field : fields) {
                if (field.includeStructure != null || field.tableTypeField) {
                    rest.add(field);
                    continue;
                }
                if (field.note != null && !field.note.isEmpty()) {
                    continue;
                }
                if (keysFirst && field.key && !isStringType(field.ddlType)) {
                    keys.add(field);
                } else {
                    rest.add(field);
                }
            }
        }
        int count = 0;
        for (DdicModel.Field field : keys) {
            emitComponent(sb, componentName(field.abapName, maxNameLength, seen), field);
            count++;
        }
        for (DdicModel.Field field : rest) {
            String lower = componentName(field.abapName, maxNameLength, seen);
            if (field.includeStructure != null) {
                String target = field.includeStructure.toLowerCase(Locale.ROOT);
                if (keysFirst) {
                    // tablolarda derin bileşen yasak: düz include kullanılır
                    sb.append("  ").append(lower).append(" : include ").append(target).append(";\n");
                } else {
                    // structure: alt yapı bileşeni (ABAP'taki "HEADER TYPE TS_HEADER" karşılığı)
                    sb.append("  ").append(lower).append(" : ").append(target).append(";\n");
                }
                count++;
                continue;
            }
            if (field.tableTypeField) {
                // structure: tabular bileşen (ABAP'taki "ITEMS TYPE TT_ITEM" karşılığı)
                if (!keysFirst && field.tableTypeName != null && !field.tableTypeName.isEmpty()) {
                    sb.append("  ").append(lower).append(" : ")
                            .append(field.tableTypeName.toLowerCase(Locale.ROOT)).append(";\n");
                    count++;
                }
                continue;
            }
            // anahtar olarak yazılmayan alanlar key işaretinden arındırılır
            // (tabloda STRING anahtar olamaz; structure'da key öneki geçersizdir)
            emitComponent(sb, lower, unkeyed(field));
            count++;
        }
        return count;
    }

    private static boolean isStringType(String ddlType) {
        if (ddlType == null) {
            return false;
        }
        String lower = ddlType.toLowerCase(Locale.ROOT);
        return lower.startsWith("abap.string") || lower.startsWith("abap.rawstring");
    }

    private static DdicModel.Field unkeyed(DdicModel.Field field) {
        DdicModel.Field copy = DdicPlanner.copyField(field);
        copy.key = false;
        return copy;
    }

    private static void emitComponent(StringBuilder sb, String lower, DdicModel.Field field) {
        String type = field.dataElementName != null
                ? field.dataElementName.toLowerCase(Locale.ROOT)
                : field.ddlType;
        sb.append("  ");
        if (field.key) {
            sb.append("key ").append(lower).append(" : ").append(type).append(" not null;\n");
        } else {
            sb.append(lower).append(" : ").append(type).append(";\n");
        }
    }

    /** DDL parser yalnızca ASCII kabul eder; Türkçe ve diğer non-ASCII karakterler çevrilir. */
    static String ascii(String value) {
        if (value == null) {
            return "";
        }
        String s = value;
        String[] from = {"ç", "Ç", "ğ", "Ğ", "ı", "İ", "ö", "Ö", "ş", "Ş", "ü", "Ü"};
        String[] to = {"c", "C", "g", "G", "i", "I", "o", "O", "s", "S", "u", "U"};
        for (int i = 0; i < from.length; i++) {
            s = s.replace(from[i], to[i]);
        }
        return s.replaceAll("[^\\x20-\\x7E]", " ");
    }

    /**
     * DDL bileşen adı: küçük harf, DDL anahtar kelimesi değil, uzunluk sınırlı,
     * mükerrer değil. Son ekler _f / _2 _3 biçimindedir.
     */
    static String componentName(String rawName, int maxLength, java.util.Set<String> seen) {
        String name = rawName == null ? "" : rawName.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(name)) {
            name = name + "_f";
        }
        name = name.replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("_+$", "");
        if (name.length() > maxLength) {
            name = name.substring(0, maxLength).replaceAll("_+$", "");
        }
        if (name.isEmpty()) {
            name = "field";
        }
        if (seen.add(name)) {
            return name;
        }
        int suffix = 2;
        while (true) {
            String candidate = trimEnd(name + "_" + suffix, maxLength);
            if (seen.add(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    private static String trimEnd(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).replaceAll("_+$", "");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return ascii(value).replace("'", "''");
    }
}
