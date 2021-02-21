package service;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
public class XmlAnalyser {
    private static final String GREEN_BOOTSTRAP_CLASS = "success";
    private static List<String> nonGreenBootstrapClasses = List.of("danger", "warning");
    private static List<String> attributesToCheck = List.of("class", "title", "href", "onclick");
    private final WebClient webClient = new WebClient();

    @SneakyThrows
    public List<String> getSimilarElements(URL originalPageUrl, URL diffPageUrl, String originalElementId) {
        log.info("Getting original element by id + {}", originalElementId);
        HtmlPage originalPage = webClient.getPage(originalPageUrl);
        DomNode originalButton = originalPage.querySelector("#" + originalElementId);
        NamedNodeMap originalAttributes = originalButton.getAttributes();
        String[] originalClasses = getOriginalClasses(originalAttributes);

        HtmlPage currentPage = webClient.getPage(diffPageUrl);
        List<DomNode> checkedNodes = new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String originalClass : originalClasses) {
            log.info("Getting diff page element by original class {}", originalClass);
            DomNodeList<DomNode> domNodesWithTheSameClass = currentPage.querySelectorAll("." + originalClass);
            for (DomNode nodeWithTheSameClass : domNodesWithTheSameClass) {
                if (checkedNodes.contains(nodeWithTheSameClass)) {
                    continue;
                }
                String nodePath = getNodePath(nodeWithTheSameClass);
                checkedNodes.add(nodeWithTheSameClass);

                log.info("Checking node {}", nodePath);
                NamedNodeMap currentAttributes = nodeWithTheSameClass.getAttributes();
                int sumPoints = getAttributesPoints(originalAttributes, currentAttributes);
                log.info("Node {} similarity points = {}", nodePath, sumPoints);

                if (sumPoints > 0) {
                    result.add(nodePath);
                }
            }
        }
        return result;
    }

    private int getAttributesPoints(NamedNodeMap originalAttributes, NamedNodeMap currentAttributes) {
        int sumPoints = 0;
        for (String attribute : attributesToCheck) {
            log.debug("Checking attribute {}", attribute);
            int attributePoint;
            if ("class".equals(attribute)){
               attributePoint = checkClasses(currentAttributes.getNamedItem("class"));
            }else{
                attributePoint = checkAttribute(currentAttributes.getNamedItem(attribute), originalAttributes.getNamedItem(attribute));
            }
            log.debug("Attribute point {}", attributePoint);
            sumPoints += attributePoint;
        }
        return sumPoints;
    }

    private String[] getOriginalClasses(NamedNodeMap originalAttributes) {
        Node originalClassNode = originalAttributes.getNamedItem("class");
        if (originalClassNode == null) {
            return new String[0];
        }
        return originalClassNode.getNodeValue().split("\\s");
    }

    private String getNodePath(DomNode node) {
        Node currentElement = node;
        StringJoiner path = new StringJoiner(" > ");

        while  (currentElement != null){
            Node aClass = currentElement.getAttributes().getNamedItem("class");
            String nodeName = currentElement.getLocalName() + (aClass == null ? "" : ".'"+aClass.getNodeValue()+"'");
            path.add(nodeName);
            currentElement = currentElement.getParentNode();
        }
        return path.toString();
    }

    private int checkAttribute(Node currentAttributeNode, Node originalAttributeNode) {
        if (areNodesEmpty(currentAttributeNode, originalAttributeNode)) return 0;
        String currentAttribute = currentAttributeNode.getNodeValue();
        String originalAttribute = originalAttributeNode.getNodeValue();

        return currentAttribute.contains(originalAttribute) ? 1 : -1;
    }

    private int checkClasses(Node currentClassNode) {
        if (currentClassNode == null){
            log.debug("Class is not present");
            return 0;
        }
        String[] currentClasses = currentClassNode.getNodeValue().split("\\s");
        for (String currentClass : currentClasses) {
            if (currentClass.contains(GREEN_BOOTSTRAP_CLASS)) {
                log.debug("Diff page class is a green Bootstrap class. Returning a point");
                return 1;
            }
            if (nonGreenBootstrapClasses.stream().anyMatch(currentClass::contains)){
                log.debug("Diff page class is a non-green Bootstrap class. Returning a minus point");
                return -1;
            }
        }
        log.debug("Diff page class cannot be analyzed. No points returned");
        return 0;
    }

    private boolean areNodesEmpty(Node currentClassNode, Node originalClassNode) {
        if (currentClassNode == null || originalClassNode == null) {
            log.debug("Attribute is not present");
            return true;
        }
        return false;
    }

}
