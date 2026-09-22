package io.github.murattahtaciii.abaparchitect.ddic;

import java.util.ArrayList;
import java.util.List;

public class DdicModel {

    public static class Field {

        public String abapName;
        public String description;
        public String ddlType;
        public String domainType;
        public int length;
        public int decimals;
        public String typeCode;
        public boolean key;
        public boolean tableTypeField;
        public Entity structEntity;
        public String includeStructure;
        public String tableTypeName;
        public String dataElementName;
        public String note = "";
    }

    public static class Entity {

        public String baseName;
        public String collectionBase;
        public String description;
        public String structureName;
        public String tableTypeName;
        public boolean arrayItem;
        public boolean root;
        public final List<Field> fields = new ArrayList<>();
    }

    private DdicModel() {
    }
}
