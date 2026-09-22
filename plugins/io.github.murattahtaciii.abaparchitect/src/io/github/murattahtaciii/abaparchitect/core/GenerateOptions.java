package io.github.murattahtaciii.abaparchitect.core;

public class GenerateOptions {

    public String root = "";
    public String structPrefix = "TS_";
    public String tablePrefix = "TT_";
    public boolean snakeCase = true;
    public String suffix = "";
    public boolean suffixToAbap = true;
    public boolean attrFlat = true;
    public boolean turkish = true;

    public String structPrefixBase() {
        return NameUtil.stripTrailingUnderscores(structPrefix);
    }

    public String tablePrefixBase() {
        return NameUtil.stripTrailingUnderscores(tablePrefix);
    }

    public String effectiveStructPrefix(String structName) {
        return structName == null || structName.isEmpty() ? structPrefixBase() : structPrefix;
    }

    public String effectiveTablePrefix(String structName) {
        return structName == null || structName.isEmpty() ? tablePrefixBase() : tablePrefix;
    }
}
