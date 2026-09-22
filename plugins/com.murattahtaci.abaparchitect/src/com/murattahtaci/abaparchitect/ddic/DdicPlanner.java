package com.murattahtaci.abaparchitect.ddic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DdicPlanner {

    private DdicPlanner() {
    }

    public static List<DdicObjectPlan> plan(DdicModel.Entity root, DdicPlanOptions options) {
        List<DdicModel.Entity> ordered = new ArrayList<>();
        assignNames(root, options, ordered);

        List<DdicObjectPlan> plans = new ArrayList<>();
        if (options.createDataElements) {
            createElements(ordered, options, plans);
        }

        for (DdicModel.Entity entity : ordered) {
            DdicObjectPlan structure = new DdicObjectPlan(DdicKind.STRUCTURE, entity.structureName,
                    entity.description);
            structure.fields = entity.fields;
            plans.add(structure);

            if (entity.arrayItem) {
                DdicObjectPlan tableType = new DdicObjectPlan(DdicKind.TABLE_TYPE, entity.tableTypeName,
                        entity.description);
                tableType.rowType = entity.structureName;
                tableType.builtinRowType = false;
                plans.add(tableType);
            }
        }

        if (options.createTable) {
            String tableName = DdicNaming.suggestTable(options, root.baseName);
            DdicObjectPlan table = new DdicObjectPlan(DdicKind.TABLE, tableName, root.description);
            table.fields = copyFields(root.fields);
            markKeyFields(table.fields);
            table.note = "Transparent tablo; " + root.fields.size() + " alan.";
            plans.add(table);
        }

        for (DdicObjectPlan plan : plans) {
            refreshSource(plan, options);
        }
        return plans;
    }

    public static void refreshSource(DdicObjectPlan plan, DdicPlanOptions options) {
        switch (plan.kind) {
            case STRUCTURE:
                plan.ddlSource = DdicDdlBuilder.structure(plan.name, plan.description, plan.fields);
                break;
            case TABLE:
                plan.ddlSource = DdicDdlBuilder.table(plan.name, plan.description, plan.fields,
                        options.addClientField);
                break;
            case TABLE_TYPE:
                plan.xmlSource = DdicXmlBuilder.tableTypeFull(plan.name, plan.description, options.packageName,
                        plan.rowType, plan.builtinRowType, options.language);
                break;
            case DATA_ELEMENT:
                plan.xmlSource = DdicXmlBuilder.dataElementFull(plan.name, plan.description, options.packageName,
                        plan.domainName, options.language);
                break;
            case DOMAIN:
                plan.xmlSource = DdicXmlBuilder.domainFull(plan.name, plan.description, options.packageName,
                        plan.domainType, plan.length, plan.decimals, options.language);
                break;
            default:
                break;
        }
    }

    public static void refreshAll(List<DdicObjectPlan> plans, DdicPlanOptions options) {
        for (DdicObjectPlan plan : plans) {
            refreshSource(plan, options);
        }
    }

    private static void createElements(List<DdicModel.Entity> ordered, DdicPlanOptions options,
            List<DdicObjectPlan> plans) {
        Map<String, DdicObjectPlan> domains = new LinkedHashMap<>();
        Map<String, DdicObjectPlan> elements = new LinkedHashMap<>();
        for (DdicModel.Entity entity : ordered) {
            for (DdicModel.Field field : entity.fields) {
                if (!isElementary(field) || "STRG".equals(field.domainType)) {
                    continue;
                }
                String domainKey = field.typeCode;
                DdicObjectPlan domain = domains.get(domainKey);
                if (domain == null) {
                    String domainName = DdicNaming.suggestDomain(options, field.typeCode);
                    domain = new DdicObjectPlan(DdicKind.DOMAIN, domainName, field.typeCode);
                    domain.domainType = field.domainType;
                    domain.length = field.length;
                    domain.decimals = field.decimals;
                    domain.note = field.domainType + " " + field.length
                            + (field.decimals > 0 ? "." + field.decimals : "");
                    domains.put(domainKey, domain);
                    plans.add(domain);
                }
                String elementKey = field.abapName + "|" + field.typeCode;
                DdicObjectPlan element = elements.get(elementKey);
                if (element == null) {
                    String elementName = DdicNaming.suggestDataElement(options, field.abapName);
                    element = new DdicObjectPlan(DdicKind.DATA_ELEMENT, elementName, field.description);
                    element.domainName = domain.name;
                    element.note = field.abapName;
                    elements.put(elementKey, element);
                    plans.add(element);
                }
                field.dataElementName = element.name;
            }
        }
    }

    /**
     * Tablo için anahtar alanı seçer: ilk elementary alan tercih edilir.
     * İlk alan STRING ise DDIC'te anahtar olamayacağı için sabit uzunluklu
     * CHAR(20)'ye çevrilir (id alanlarının anahtar olması beklenir).
     */
    private static void markKeyFields(List<DdicModel.Field> fields) {
        for (DdicModel.Field field : fields) {
            if (isElementary(field)) {
                if ("STRG".equals(field.domainType)) {
                    field.ddlType = "abap.char(20)";
                    field.domainType = "CHAR";
                    field.length = 20;
                    field.decimals = 0;
                    field.typeCode = "CHAR20";
                    field.note = "";
                }
                field.key = true;
                return;
            }
        }
        for (DdicModel.Field field : fields) {
            if (field.includeStructure == null && !field.tableTypeField) {
                field.key = true;
                return;
            }
        }
    }

    public static List<DdicModel.Field> copyFields(List<DdicModel.Field> source) {
        List<DdicModel.Field> copies = new ArrayList<>();
        if (source == null) {
            return copies;
        }
        for (DdicModel.Field field : source) {
            copies.add(copyField(field));
        }
        return copies;
    }

    public static DdicModel.Field copyField(DdicModel.Field source) {
        DdicModel.Field copy = new DdicModel.Field();
        copy.abapName = source.abapName;
        copy.description = source.description;
        copy.ddlType = source.ddlType;
        copy.domainType = source.domainType;
        copy.length = source.length;
        copy.decimals = source.decimals;
        copy.typeCode = source.typeCode;
        copy.key = source.key;
        copy.tableTypeField = source.tableTypeField;
        copy.structEntity = source.structEntity;
        copy.includeStructure = source.includeStructure;
        copy.tableTypeName = source.tableTypeName;
        copy.dataElementName = source.dataElementName;
        copy.note = source.note;
        return copy;
    }

    private static boolean isElementary(DdicModel.Field field) {
        return field.structEntity == null && !field.tableTypeField;
    }

    private static void assignNames(DdicModel.Entity entity, DdicPlanOptions options,
            List<DdicModel.Entity> ordered) {
        entity.structureName = DdicNaming.suggestStructure(options, entity.baseName);
        if (entity.arrayItem) {
            String base = entity.collectionBase != null ? entity.collectionBase : entity.baseName;
            entity.tableTypeName = DdicNaming.suggestTableType(options, base);
        }
        for (DdicModel.Field field : entity.fields) {
            if (field.structEntity != null) {
                assignNames(field.structEntity, options, ordered);
                if (field.tableTypeField) {
                    field.tableTypeName = field.structEntity.tableTypeName;
                } else {
                    field.includeStructure = field.structEntity.structureName;
                }
            }
        }
        ordered.add(entity);
    }

    public static String preview(List<DdicObjectPlan> plans) {
        StringBuilder sb = new StringBuilder();
        sb.append("* DDIC Oluşturma Planı (").append(plans.size()).append(" nesne)\n\n");
        for (DdicObjectPlan plan : plans) {
            sb.append("- ").append(plan.kind.name()).append("  ").append(plan.name);
            if (!plan.note.isEmpty()) {
                sb.append("  (").append(plan.note).append(")");
            }
            String error = plan.validationError();
            if (error != null) {
                sb.append("  !! ").append(error);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
