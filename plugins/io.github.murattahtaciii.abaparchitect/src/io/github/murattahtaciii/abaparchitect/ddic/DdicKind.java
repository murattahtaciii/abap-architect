package io.github.murattahtaciii.abaparchitect.ddic;

public enum DdicKind {

    STRUCTURE("TABL/DS", "structures", "application/vnd.sap.adt.structures.v2+xml", true, 30),
    TABLE("TABL/DT", "tables", "application/vnd.sap.adt.tables.v2+xml", true, 16),
    TABLE_TYPE("TTYP/DA", "tabletypes", "application/vnd.sap.adt.tabletype.v1+xml", false, 30),
    DATA_ELEMENT("DTEL/DE", "dataelements", "application/vnd.sap.adt.dataelements.v2+xml", false, 30),
    DOMAIN("DOMA/DD", "domains", "application/vnd.sap.adt.domains.v2+xml", false, 30);

    public final String adtType;
    public final String uriSegment;
    public final String contentType;
    public final boolean ddlSource;
    public final int maxLength;

    DdicKind(String adtType, String uriSegment, String contentType, boolean ddlSource, int maxLength) {
        this.adtType = adtType;
        this.uriSegment = uriSegment;
        this.contentType = contentType;
        this.ddlSource = ddlSource;
        this.maxLength = maxLength;
    }
}
