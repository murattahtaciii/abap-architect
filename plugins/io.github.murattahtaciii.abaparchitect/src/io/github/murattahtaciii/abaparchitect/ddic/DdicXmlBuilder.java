package io.github.murattahtaciii.abaparchitect.ddic;

public final class DdicXmlBuilder {

    /** Table type satır tipi olarak kullanılabilen yerleşik ABAP tipleri. */
    public static final java.util.List<String> BUILTIN_ROW_TYPES = java.util.List.of(
            "STRING", "XSTRING", "I", "INT8", "F", "P", "D", "T", "C", "N", "X",
            "DECFLOAT16", "DECFLOAT34", "UTCLONG");

    private DdicXmlBuilder() {
    }

    public static String objectUri(DdicKind kind, String name) {
        return "/sap/bc/adt/ddic/" + kind.uriSegment + "/" + name.toLowerCase(java.util.Locale.ROOT);
    }

    public static String structureCreate(String name, String description, String packageName, String language) {
        return blueSource("TABL/DS", name, description, packageName, language);
    }

    public static String tableCreate(String name, String description, String packageName, String language) {
        return blueSource("TABL/DT", name, description, packageName, language);
    }

    private static String blueSource(String type, String name, String description, String packageName,
            String language) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<blue:blueSource xmlns:blue=\"http://www.sap.com/wbobj/blue\""
                + " xmlns:adtcore=\"http://www.sap.com/adt/core\""
                + " adtcore:description=\"" + escape(description) + "\""
                + " adtcore:language=\"" + language + "\""
                + " adtcore:name=\"" + name + "\""
                + " adtcore:type=\"" + type + "\""
                + " adtcore:masterLanguage=\"" + language + "\">"
                + "<adtcore:packageRef adtcore:name=\"" + packageName + "\"/>"
                + "</blue:blueSource>";
    }

    public static String tableTypeCreate(String name, String description, String packageName, String language) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ttyp:tableType xmlns:ttyp=\"http://www.sap.com/dictionary/tabletype\""
                + " xmlns:adtcore=\"http://www.sap.com/adt/core\""
                + " adtcore:description=\"" + escape(description) + "\""
                + " adtcore:language=\"" + language + "\""
                + " adtcore:name=\"" + name + "\""
                + " adtcore:type=\"TTYP/DA\""
                + " adtcore:masterLanguage=\"" + language + "\">"
                + "<adtcore:packageRef adtcore:name=\"" + packageName + "\"/>"
                + "</ttyp:tableType>";
    }

    public static String tableTypeFull(String name, String description, String packageName, String rowType,
            boolean builtinRowType, String language) {
        String rowTypeXml = builtinRowType
                ? "<ttyp:typeKind>predefinedAbapType</ttyp:typeKind><ttyp:typeName/>"
                        + "<ttyp:builtInType><ttyp:dataType>" + escape(rowType)
                        + "</ttyp:dataType><ttyp:length>000000</ttyp:length>"
                        + "<ttyp:decimals>000000</ttyp:decimals></ttyp:builtInType><ttyp:rangeType/>"
                : "<ttyp:typeKind>dictionaryType</ttyp:typeKind><ttyp:typeName>" + escape(rowType)
                        + "</ttyp:typeName><ttyp:builtInType><ttyp:dataType>STRU</ttyp:dataType>"
                        + "<ttyp:length>000000</ttyp:length><ttyp:decimals>000000</ttyp:decimals>"
                        + "</ttyp:builtInType><ttyp:rangeType/>";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ttyp:tableType xmlns:ttyp=\"http://www.sap.com/dictionary/tabletype\""
                + " xmlns:adtcore=\"http://www.sap.com/adt/core\""
                + " adtcore:description=\"" + escape(description) + "\""
                + " adtcore:name=\"" + name + "\""
                + " adtcore:type=\"TTYP/DA\""
                + " adtcore:masterLanguage=\"" + language + "\">"
                + "<adtcore:packageRef adtcore:name=\"" + packageName + "\"/>"
                + "<ttyp:rowType>" + rowTypeXml + "</ttyp:rowType>"
                + "<ttyp:initialRowCount>00000</ttyp:initialRowCount>"
                + "<ttyp:accessType>standard</ttyp:accessType>"
                + "<ttyp:primaryKey ttyp:isVisible=\"true\" ttyp:isEditable=\"true\">"
                + "<ttyp:definition>standard</ttyp:definition><ttyp:kind>nonUnique</ttyp:kind>"
                + "<ttyp:components ttyp:isVisible=\"false\"/><ttyp:alias/></ttyp:primaryKey>"
                + "<ttyp:secondaryKeys ttyp:isVisible=\"true\" ttyp:isEditable=\"true\">"
                + "<ttyp:allowed>notSpecified</ttyp:allowed></ttyp:secondaryKeys>"
                + "</ttyp:tableType>";
    }

    public static String domainCreate(String name, String description, String packageName, String language) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<doma:domain xmlns:doma=\"http://www.sap.com/dictionary/domain\""
                + " xmlns:adtcore=\"http://www.sap.com/adt/core\""
                + " adtcore:description=\"" + escape(description) + "\""
                + " adtcore:language=\"" + language + "\""
                + " adtcore:name=\"" + name + "\""
                + " adtcore:type=\"DOMA/DD\""
                + " adtcore:masterLanguage=\"" + language + "\">"
                + "<adtcore:packageRef adtcore:name=\"" + packageName + "\"/>"
                + "</doma:domain>";
    }

    public static String domainFull(String name, String description, String packageName, String dataType,
            int length, int decimals, String language) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<doma:domain xmlns:doma=\"http://www.sap.com/dictionary/domain\""
                + " xmlns:adtcore=\"http://www.sap.com/adt/core\""
                + " adtcore:description=\"" + escape(description) + "\""
                + " adtcore:name=\"" + name + "\""
                + " adtcore:type=\"DOMA/DD\""
                + " adtcore:masterLanguage=\"" + language + "\">"
                + "<adtcore:packageRef adtcore:name=\"" + packageName + "\"/>"
                + "<doma:content>"
                + "<doma:typeInformation>"
                + "<doma:datatype>" + escape(dataType) + "</doma:datatype>"
                + "<doma:length>" + pad6(length) + "</doma:length>"
                + "<doma:decimals>" + pad6(decimals) + "</doma:decimals>"
                + "</doma:typeInformation>"
                + "<doma:outputInformation>"
                + "<doma:length>" + pad6(length) + "</doma:length>"
                + "<doma:style>00</doma:style>"
                + "<doma:conversionExit></doma:conversionExit>"
                + "<doma:signExists>false</doma:signExists>"
                + "<doma:lowercase>false</doma:lowercase>"
                + "<doma:ampmFormat>false</doma:ampmFormat>"
                + "</doma:outputInformation>"
                + "<doma:valueInformation>"
                + "<doma:appendExists>false</doma:appendExists>"
                + "<doma:fixValues/>"
                + "</doma:valueInformation>"
                + "</doma:content>"
                + "</doma:domain>";
    }

    public static String dataElementCreate(String name, String description, String packageName, String language) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<blue:wbobj xmlns:blue=\"http://www.sap.com/wbobj/dictionary/dtel\""
                + " xmlns:adtcore=\"http://www.sap.com/adt/core\""
                + " adtcore:description=\"" + escape(description) + "\""
                + " adtcore:language=\"" + language + "\""
                + " adtcore:name=\"" + name + "\""
                + " adtcore:type=\"DTEL/DE\""
                + " adtcore:masterLanguage=\"" + language + "\">"
                + "<adtcore:packageRef adtcore:name=\"" + packageName + "\"/>"
                + "</blue:wbobj>";
    }

    public static String dataElementFull(String name, String description, String packageName, String domainName,
            String language) {
        String shortLabel = shorten(description, 10);
        String mediumLabel = shorten(description, 20);
        String longLabel = shorten(description, 40);
        String headingLabel = shorten(description, 55);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<blue:wbobj xmlns:blue=\"http://www.sap.com/wbobj/dictionary/dtel\""
                + " xmlns:adtcore=\"http://www.sap.com/adt/core\""
                + " adtcore:description=\"" + escape(description) + "\""
                + " adtcore:name=\"" + name + "\""
                + " adtcore:type=\"DTEL/DE\""
                + " adtcore:masterLanguage=\"" + language + "\">"
                + "<adtcore:packageRef adtcore:name=\"" + packageName + "\"/>"
                + "<dtel:dataElement xmlns:dtel=\"http://www.sap.com/adt/dictionary/dataelements\">"
                + "<dtel:typeKind>domain</dtel:typeKind>"
                + "<dtel:typeName>" + escape(domainName) + "</dtel:typeName>"
                + "<dtel:dataType></dtel:dataType>"
                + "<dtel:dataTypeLength>000000</dtel:dataTypeLength>"
                + "<dtel:dataTypeDecimals>000000</dtel:dataTypeDecimals>"
                + "<dtel:shortFieldLabel>" + escape(shortLabel) + "</dtel:shortFieldLabel>"
                + "<dtel:shortFieldLength>" + pad2(shortLabel.length()) + "</dtel:shortFieldLength>"
                + "<dtel:shortFieldMaxLength>10</dtel:shortFieldMaxLength>"
                + "<dtel:mediumFieldLabel>" + escape(mediumLabel) + "</dtel:mediumFieldLabel>"
                + "<dtel:mediumFieldLength>" + pad2(mediumLabel.length()) + "</dtel:mediumFieldLength>"
                + "<dtel:mediumFieldMaxLength>20</dtel:mediumFieldMaxLength>"
                + "<dtel:longFieldLabel>" + escape(longLabel) + "</dtel:longFieldLabel>"
                + "<dtel:longFieldLength>" + pad2(longLabel.length()) + "</dtel:longFieldLength>"
                + "<dtel:longFieldMaxLength>40</dtel:longFieldMaxLength>"
                + "<dtel:headingFieldLabel>" + escape(headingLabel) + "</dtel:headingFieldLabel>"
                + "<dtel:headingFieldLength>" + pad2(headingLabel.length()) + "</dtel:headingFieldLength>"
                + "<dtel:headingFieldMaxLength>55</dtel:headingFieldMaxLength>"
                + "<dtel:searchHelp></dtel:searchHelp>"
                + "<dtel:searchHelpParameter></dtel:searchHelpParameter>"
                + "<dtel:setGetParameter></dtel:setGetParameter>"
                + "<dtel:defaultComponentName></dtel:defaultComponentName>"
                + "<dtel:deactivateInputHistory>false</dtel:deactivateInputHistory>"
                + "<dtel:changeDocument>false</dtel:changeDocument>"
                + "<dtel:leftToRightDirection>false</dtel:leftToRightDirection>"
                + "<dtel:deactivateBIDIFiltering>false</dtel:deactivateBIDIFiltering>"
                + "</dtel:dataElement>"
                + "</blue:wbobj>";
    }

    public static String activation(String uri, String name) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<adtcore:objectReferences xmlns:adtcore=\"http://www.sap.com/adt/core\">"
                + "<adtcore:objectReference adtcore:uri=\"" + escape(uri) + "\" adtcore:name=\"" + name + "\"/>"
                + "</adtcore:objectReferences>";
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String pad6(int value) {
        return String.format("%06d", value);
    }

    private static String pad2(int value) {
        return String.format("%02d", value);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
