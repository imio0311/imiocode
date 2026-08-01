package io.imiocode.context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/** 严格解析摘要协议，拒绝围栏、根外文本和结构注入。 */
public final class SummaryParser {
    public ParsedSummary parse(String raw) throws ContextException {
        if (raw == null || raw.isBlank() || raw.length() > ContextPolicy.MAX_SUMMARY_RESPONSE_CHARS) {
            throw invalid();
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("<summary>") || !trimmed.endsWith("</summary>")) throw invalid();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(trimmed)));
            Element root = document.getDocumentElement();
            if (!"summary".equals(root.getTagName()) || root.hasAttributes()) throw invalid();
            List<Element> children = childElements(root, true);
            if (children.size() != 2 || !"prior_history".equals(children.get(0).getTagName())
                    || !"active_task".equals(children.get(1).getTagName())) throw invalid();
            for (Element child : children) if (child.hasAttributes() || !childElements(child, false).isEmpty()) throw invalid();
            return new ParsedSummary(children.get(0).getTextContent(), children.get(1).getTextContent());
        } catch (ContextException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ContextException("摘要格式无效，已保留原上下文", true, exception);
        }
    }

    private static List<Element> childElements(Element parent, boolean rejectText) throws ContextException {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) result.add(element);
            else if (rejectText && node.getNodeType() == Node.TEXT_NODE && !node.getTextContent().isBlank()) throw invalid();
        }
        return result;
    }

    private static ContextException invalid() {
        return new ContextException("摘要格式无效，已保留原上下文", true);
    }
}
