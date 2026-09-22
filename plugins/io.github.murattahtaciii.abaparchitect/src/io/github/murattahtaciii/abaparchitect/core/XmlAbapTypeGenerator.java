package io.github.murattahtaciii.abaparchitect.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

public class XmlAbapTypeGenerator {

    private static final Pattern INTEGER = Pattern.compile("^-?\\d+$");
    private static final Pattern DECIMAL = Pattern.compile("^-?\\d+\\.\\d+$");
    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME = Pattern.compile("^\\d{2}:\\d{2}:\\d{2}$");

    private final GenerateOptions options;
    private final List<String> typeDefs = new ArrayList<>();
    private final Map<String, String> definedStructures = new LinkedHashMap<>();

    public XmlAbapTypeGenerator(GenerateOptions options) {
        this.options = options;
    }

    public List<String> getTypeDefs() {
        return typeDefs;
    }

    public String parseElement(Element element, String name, boolean isArrayItem) {
        List<Element> childElements = XmlSupport.childElements(element);
        List<XmlSupport.Attr> attributes = XmlSupport.attributes(element);
        boolean textOnly = childElements.isEmpty();

        if (textOnly && attributes.isEmpty()) {
            return inferType(XmlSupport.text(element));
        }

        List<String> lines = new ArrayList<>();
        StringBuilder signature = new StringBuilder();

        if (!attributes.isEmpty()) {
            if (options.attrFlat) {
                for (XmlSupport.Attr attr : attributes) {
                    String fieldName = NameUtil.toV(attr.name, options.snakeCase);
                    String line = "    " + NameUtil.padEnd(fieldName, 25) + " TYPE string,  \" @" + attr.name;
                    lines.add(line);
                    signature.append(line.trim());
                }
            } else {
                List<String> attrLines = new ArrayList<>();
                for (XmlSupport.Attr attr : attributes) {
                    String fieldName = NameUtil.toV(attr.name, options.snakeCase);
                    attrLines.add("    " + NameUtil.padEnd(fieldName, 25) + " TYPE string,");
                }
                String attrStructName = options.structPrefix + NameUtil.toV(name, options.snakeCase) + "_ATTR";
                typeDefs.add("TYPES:\n  BEGIN OF " + attrStructName + ",\n"
                        + String.join("\n", attrLines)
                        + "\n  END OF " + attrStructName + ".");
                String line = NameUtil.padEnd("    ATTRIBUTES", 29) + " TYPE " + attrStructName + ",";
                lines.add(line);
                signature.append(line.trim());
            }
        }

        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        for (Element child : childElements) {
            tagCounts.merge(child.getTagName(), 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> tagEntry : tagCounts.entrySet()) {
            String tagName = tagEntry.getKey();
            boolean isArray = tagEntry.getValue() > 1;
            Element child = firstWithTag(childElements, tagName);
            if (child == null) {
                continue;
            }
            String fieldName = NameUtil.toV(tagName, options.snakeCase);
            String line;
            if (isArray) {
                String singular = NameUtil.makeSingular(tagName);
                String elementType = parseElement(child, singular, true);
                String structBase = options.structPrefixBase();
                if (elementType.startsWith("TYPE ") && elementType.substring(5).startsWith(structBase)) {
                    String typeName = elementType.substring(5).trim();
                    String currentStructPrefix = typeName.contains(options.structPrefix) ? options.structPrefix : structBase;
                    String currentTablePrefix = currentStructPrefix.equals(options.structPrefix)
                            ? options.tablePrefix
                            : options.tablePrefixBase();
                    line = "    " + NameUtil.padEnd(fieldName, 25) + " TYPE "
                            + NameUtil.replaceFirst(typeName, currentStructPrefix, currentTablePrefix) + ",";
                } else {
                    line = "    " + NameUtil.padEnd(fieldName, 25) + " TYPE STANDARD TABLE OF "
                            + elementType.substring(5) + " WITH EMPTY KEY,";
                }
            } else {
                String childType = parseElement(child, tagName, false);
                line = "    " + NameUtil.padEnd(fieldName, 25) + " " + childType + ",";
            }
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

    private Element firstWithTag(List<Element> elements, String tagName) {
        for (Element element : elements) {
            if (tagName.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private String inferType(String text) {
        if (text.isEmpty()) {
            return "TYPE string";
        }
        if ("true".equals(text) || "false".equals(text)) {
            return "TYPE abap_bool";
        }
        if (DATE.matcher(text).matches()) {
            return "TYPE d";
        }
        if (TIME.matcher(text).matches()) {
            return "TYPE t";
        }
        if (INTEGER.matcher(text).matches()) {
            try {
                long value = Long.parseLong(text);
                return Math.abs(value) > 2_000_000_000L ? "TYPE p LENGTH 16 DECIMALS 0" : "TYPE i";
            } catch (NumberFormatException e) {
                return "TYPE p LENGTH 16 DECIMALS 0";
            }
        }
        if (DECIMAL.matcher(text).matches()) {
            return "TYPE p LENGTH 16 DECIMALS 3";
        }
        return "TYPE string";
    }
}
