package io.github.murattahtaciii.abaparchitect.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AbapTypeGenerator {

    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$");

    private final GenerateOptions options;
    private final List<String> typeDefs = new ArrayList<>();
    private final List<String> mapLines = new ArrayList<>();
    private final Map<String, String> definedStructures = new LinkedHashMap<>();

    public AbapTypeGenerator(GenerateOptions options) {
        this.options = options;
    }

    public List<String> getTypeDefs() {
        return typeDefs;
    }

    public List<String> getMapLines() {
        return mapLines;
    }

    public String parseNode(Object node, String name, boolean isArrayItem) {
        if (node == null) {
            return "TYPE string";
        }
        if (node instanceof String) {
            return parseString((String) node);
        }
        if (node instanceof Boolean) {
            return "TYPE abap_bool";
        }
        if (node instanceof Number) {
            return parseNumber((Number) node);
        }
        if (node instanceof List) {
            return parseArray((List<?>) node, name);
        }
        if (node instanceof Map) {
            return parseObject((Map<?, ?>) node, name, isArrayItem);
        }
        return "TYPE string";
    }

    private String parseString(String value) {
        if (DATE.matcher(value).matches()) {
            return "TYPE d";
        }
        if (TIME.matcher(value).matches()) {
            return "TYPE t";
        }
        return "TYPE string";
    }

    private String parseNumber(Number number) {
        Object integral = number;
        if (number instanceof java.math.BigDecimal) {
            java.math.BigDecimal decimal = ((java.math.BigDecimal) number).stripTrailingZeros();
            if (decimal.scale() > 0) {
                return "TYPE p LENGTH 16 DECIMALS 3";
            }
            integral = decimal.toBigIntegerExact();
        } else if (!JsonWriter.isInteger(number)) {
            return "TYPE p LENGTH 16 DECIMALS 3";
        }
        return JsonWriter.exceedsInt32(integral) ? "TYPE p LENGTH 16 DECIMALS 0" : "TYPE i";
    }

    private String parseArray(List<?> node, String name) {
        if (node.isEmpty()) {
            return "TYPE STANDARD TABLE OF string WITH EMPTY KEY";
        }
        String singular = NameUtil.makeSingular(name == null || name.isEmpty() ? "ITEM" : name);
        String elementType = parseNode(node.get(0), singular, true);
        String structBase = options.structPrefixBase();
        if (elementType.startsWith("TYPE ") && elementType.substring(5).startsWith(structBase)) {
            String typeName = elementType.substring(5).trim();
            String currentStructPrefix = typeName.contains(options.structPrefix) ? options.structPrefix : structBase;
            String currentTablePrefix = currentStructPrefix.equals(options.structPrefix)
                    ? options.tablePrefix
                    : options.tablePrefixBase();
            return "TYPE " + NameUtil.replaceFirst(typeName, currentStructPrefix, currentTablePrefix);
        }
        return "TYPE STANDARD TABLE OF " + elementType.substring(5) + " WITH EMPTY KEY";
    }

    private String parseObject(Map<?, ?> node, String name, boolean isArrayItem) {
        List<String> lines = new ArrayList<>();
        StringBuilder signature = new StringBuilder();

        for (Map.Entry<?, ?> entry : node.entrySet()) {
            String jsonKey = String.valueOf(entry.getKey());
            String abapKey = NameUtil.toV(jsonKey, options.snakeCase);
            String mappedJsonKey = jsonKey;

            if (options.suffix != null && !options.suffix.isEmpty()) {
                String suffixUpper = options.suffix.toUpperCase(java.util.Locale.ROOT);
                if (options.suffixToAbap) {
                    if (!abapKey.endsWith(suffixUpper)) {
                        abapKey = abapKey + suffixUpper;
                    }
                } else {
                    mappedJsonKey = jsonKey + options.suffix;
                }
            }
            if (!abapKey.equals(jsonKey) || !mappedJsonKey.equals(jsonKey)) {
                mapLines.add("    ( abap = '" + abapKey + "' json = '" + mappedJsonKey + "' )");
            }

            String childType = parseNode(entry.getValue(), jsonKey, false);
            String line = "    " + NameUtil.padEnd(abapKey, 25) + " " + childType + ",";
            lines.add(line);
            signature.append(line.trim());
        }

        String baseName = NameUtil.toV(name, options.snakeCase);
        String currentStructPrefix = options.effectiveStructPrefix(baseName);
        String currentTablePrefix = options.effectiveTablePrefix(baseName);
        String shortName = currentStructPrefix + baseName;
        String finalName = shortName;
        int counter = 2;
        while (true) {
            String existing = definedStructures.get(finalName);
            if (existing == null) {
                definedStructures.put(finalName, signature.toString());
                break;
            }
            if (existing.equals(signature.toString())) {
                return "TYPE " + finalName;
            }
            finalName = shortName + "_V" + (counter++);
        }

        String tableTypeName = NameUtil.replaceFirst(finalName, currentStructPrefix, currentTablePrefix);
        StringBuilder definition = new StringBuilder();
        definition.append("TYPES:\n  BEGIN OF ").append(finalName).append(",\n");
        definition.append(String.join("\n", lines));
        if (isArrayItem) {
            definition.append("\n  END OF ").append(finalName).append(",\n");
            definition.append("  ").append(tableTypeName)
                    .append(" TYPE STANDARD TABLE OF ").append(finalName).append(" WITH EMPTY KEY.");
        } else {
            definition.append("\n  END OF ").append(finalName).append('.');
        }
        typeDefs.add(definition.toString());
        return "TYPE " + finalName;
    }
}
