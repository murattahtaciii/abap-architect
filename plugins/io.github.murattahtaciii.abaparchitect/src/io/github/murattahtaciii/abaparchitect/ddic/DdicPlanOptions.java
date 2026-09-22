package io.github.murattahtaciii.abaparchitect.ddic;

public class DdicPlanOptions {

    /** Modül ön eki, örn. ZFI, ZSD_ */
    public String modulePrefix = "Z";

    /** Nesne kodu parçaları: isim <modül>_<kod>_<ad> biçiminde üretilir. */
    public String structureCode = "S";
    public String tableTypeCode = "TT";
    public String tableCode = "T";
    public String elementCode = "DE";
    public String domainCode = "DD";

    public String packageName = "$TMP";
    public String transport = "";

    public boolean snakeCase = true;
    public boolean createTable = false;
    public boolean createDataElements = false;
    public boolean addClientField = true;
    public String language = "EN";
    public boolean turkish = true;
}
