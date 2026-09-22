package io.github.murattahtaciii.abaparchitect.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

public class SimpleTransformationGenerator {

    private final GenerateOptions options;

    public SimpleTransformationGenerator(GenerateOptions options) {
        this.options = options;
    }

    public String generate(Element rootElement) {
        StringBuilder st = new StringBuilder();
        st.append("<?sap.transform simple?>\n");
        st.append("<tt:transform xmlns:tt=\"http://www.sap.com/transformation-templates\">\n");
        st.append("  <tt:root name=\"ROOT\"/>\n");
        st.append("  <tt:template>\n");
        st.append(element(rootElement, "ROOT", 2));
        st.append("  </tt:template>\n");
        st.append("</tt:transform>");
        return st.toString();
    }

    private String element(Element element, String refPath, int depth) {
        String indent = "    ".repeat(depth);
        List<Element> childElements = XmlSupport.childElements(element);
        List<XmlSupport.Attr> attributes = XmlSupport.attributes(element);
        StringBuilder st = new StringBuilder();

        st.append(indent).append('<').append(element.getTagName()).append(">\n");
        for (XmlSupport.Attr attr : attributes) {
            String field = NameUtil.toV(attr.name, options.snakeCase);
            st.append(indent).append("  <tt:attribute name=\"").append(attr.name)
                    .append("\" value-ref=\"").append(refPath).append('.').append(field).append("\"/>\n");
        }
        if (!attributes.isEmpty() && childElements.isEmpty()) {
            st.append(indent).append("</").append(element.getTagName()).append(">\n");
            return st.toString();
        }

        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        for (Element child : childElements) {
            tagCounts.merge(child.getTagName(), 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> tagEntry : tagCounts.entrySet()) {
            String tagName = tagEntry.getKey();
            Element child = firstWithTag(childElements, tagName);
            if (child == null) {
                continue;
            }
            String field = NameUtil.toV(tagName, options.snakeCase);
            boolean isArray = tagEntry.getValue() > 1;
            boolean hasKids = !XmlSupport.childElements(child).isEmpty()
                    || !XmlSupport.attributes(child).isEmpty();

            if (isArray) {
                st.append(indent).append("  <tt:loop ref=\"").append(refPath).append('.').append(field).append("\">\n");
                if (hasKids) {
                    st.append(element(child, "$ref", depth + 2));
                } else {
                    st.append(indent).append("    <").append(tagName).append(">\n");
                    st.append(indent).append("      <tt:value ref=\"$ref\"/>\n");
                    st.append(indent).append("    </").append(tagName).append(">\n");
                }
                st.append(indent).append("  </tt:loop>\n");
            } else if (hasKids) {
                st.append(element(child, refPath + "." + field, depth + 1));
            } else {
                st.append(indent).append("  <").append(tagName).append(">\n");
                st.append(indent).append("    <tt:value ref=\"").append(refPath).append('.').append(field).append("\"/>\n");
                st.append(indent).append("  </").append(tagName).append(">\n");
            }
        }
        st.append(indent).append("</").append(element.getTagName()).append(">\n");
        return st.toString();
    }

    private Element firstWithTag(List<Element> elements, String tagName) {
        for (Element element : elements) {
            if (tagName.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }
}
