package io.github.murattahtaciii.abaparchitect.ddic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import io.github.murattahtaciii.abaparchitect.core.NameUtil;
import io.github.murattahtaciii.abaparchitect.core.XmlSupport;

public final class DdicModelBuilder {

    private DdicModelBuilder() {
    }

    public static DdicModel.Entity fromJson(Object node, String baseName, String description, DdicPlanOptions options) {
        DdicModel.Entity entity = new DdicModel.Entity();
        entity.baseName = baseName;
        entity.description = description;
        if (!(node instanceof Map)) {
            return entity;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            DdicModel.Field field = new DdicModel.Field();
            field.abapName = NameUtil.toV(key, options.snakeCase);
            field.description = key;

            if (value instanceof Map) {
                field.structEntity = fromJson(value, baseName + "_" + field.abapName, key, options);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    String singular = NameUtil.makeSingular(field.abapName);
                    DdicModel.Entity item = fromJson(list.get(0), baseName + "_" + singular, key, options);
                    item.arrayItem = true;
                    item.collectionBase = baseName + "_" + field.abapName;
                    field.tableTypeField = true;
                    field.structEntity = item;
                } else {
                    field.note = "Basit tip dizisi; DDIC structure bileşeni olamaz.";
                }
            } else if (value instanceof Boolean) {
                applyType(field, AbapTypeMapping.bool());
            } else if (value instanceof Number) {
                applyType(field, AbapTypeMapping.forNumber((Number) value));
            } else {
                applyType(field, AbapTypeMapping.forText(String.valueOf(value)));
            }
            entity.fields.add(field);
        }
        return entity;
    }

    public static DdicModel.Entity fromXml(Element element, String baseName, String description,
            DdicPlanOptions options) {
        DdicModel.Entity entity = new DdicModel.Entity();
        entity.baseName = baseName;
        entity.description = description;

        for (XmlSupport.Attr attr : XmlSupport.attributes(element)) {
            DdicModel.Field field = new DdicModel.Field();
            field.abapName = NameUtil.toV(attr.name, options.snakeCase);
            field.description = "@" + attr.name;
            applyType(field, AbapTypeMapping.string());
            entity.fields.add(field);
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Element child : XmlSupport.childElements(element)) {
            counts.merge(child.getTagName(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String tag = entry.getKey();
            Element child = firstWithTag(element, tag);
            if (child == null) {
                continue;
            }
            String abapName = NameUtil.toV(tag, options.snakeCase);
            boolean repeated = entry.getValue() > 1;
            boolean leaf = XmlSupport.childElements(child).isEmpty() && XmlSupport.attributes(child).isEmpty();
            DdicModel.Field field = new DdicModel.Field();
            field.abapName = abapName;
            field.description = tag;

            if (repeated && !leaf) {
                String singular = NameUtil.makeSingular(abapName);
                DdicModel.Entity item = fromXml(child, baseName + "_" + singular, tag, options);
                item.arrayItem = true;
                item.collectionBase = baseName + "_" + abapName;
                field.tableTypeField = true;
                field.structEntity = item;
            } else if (leaf) {
                applyType(field, AbapTypeMapping.forText(XmlSupport.text(child), true));
            } else {
                field.structEntity = fromXml(child, baseName + "_" + abapName, tag, options);
            }
            entity.fields.add(field);
        }
        return entity;
    }

    private static void applyType(DdicModel.Field field, AbapTypeMapping.TypeInfo info) {
        field.ddlType = info.ddlType;
        field.domainType = info.domainType;
        field.length = info.length;
        field.decimals = info.decimals;
        field.typeCode = info.code;
    }

    private static Element firstWithTag(Element parent, String tag) {
        for (Element child : XmlSupport.childElements(parent)) {
            if (tag.equals(child.getTagName())) {
                return child;
            }
        }
        return null;
    }
}
