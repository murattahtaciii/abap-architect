package com.murattahtaci.abaparchitect.ddic;

public class DdicObjectPlan {

    public DdicKind kind;
    public String name;
    public String description;
    public String ddlSource;
    public String xmlSource;
    public String rowType;
    public boolean builtinRowType;
    public boolean selected = true;
    public String note = "";
    public java.util.List<DdicModel.Field> fields;
    public String domainName;
    public String domainType;
    public int length;
    public int decimals;

    public DdicObjectPlan(DdicKind kind, String name, String description) {
        this.kind = kind;
        this.name = name;
        this.description = description;
    }

    public String validationError() {
        return DdicNaming.validate(name, kind);
    }
}
