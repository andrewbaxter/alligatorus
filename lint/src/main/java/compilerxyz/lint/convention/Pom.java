package compilerxyz.lint.convention;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class Pom {
    private final Document document;

    public Pom(Path pomFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            this.document = factory.newDocumentBuilder().parse(pomFile.toFile());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("cannot parse " + pomFile, e);
        }
    }

    public List<String> modules() {
        List<String> out = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("module");
        for (int i = 0; i < nodes.getLength(); i++) {
            out.add(nodes.item(i).getTextContent().strip());
        }
        return out;
    }

    public Set<String> artifactIds() {
        Set<String> out = new LinkedHashSet<>();
        NodeList nodes = document.getElementsByTagName("artifactId");
        for (int i = 0; i < nodes.getLength(); i++) {
            out.add(nodes.item(i).getTextContent().strip());
        }
        return out;
    }
}
