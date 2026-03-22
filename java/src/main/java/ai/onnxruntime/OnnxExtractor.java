package ai.onnxruntime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class OnnxExtractor {

    private static final String XML_PATH = "META-INF/onnxruntime/extract.xml";

    private static boolean didRun;

    static void run() throws IOException {
        if (didRun) {
            return;
        }
        for (Library library : fetchExtractionTargets()) {
            for (Entry entry : library.getEntries()) {
                if (entry.shouldLoad()) {
                    OnnxRuntime.load(library.getPrefix(), library.getDirectory(), library.getName());
                } else {
                    OnnxRuntime.extractFromResources(library.getDirectory(), entry.getName());
                }
            }
        }
    }

    private static List<Library> fetchExtractionTargets() throws IOException {
        Enumeration<URL> resources = OnnxExtractor.class.getClassLoader().getResources(XML_PATH);
        List<Library> libraries = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resourceUrl = resources.nextElement();
            try (InputStream is = resourceUrl.openStream()) {
                libraries.add(parseXml(is));
            } catch (SAXException | ParserConfigurationException e) {
                throw new IOException(e);
            }
        }
        return libraries;
    }

    private static Library parseXml(InputStream xmlInput)
            throws SAXException, IOException, ParserConfigurationException {
        Element libElement = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(xmlInput).getDocumentElement();
        String name = libElement.getAttribute("name");
        String prefix = libElement.getAttribute("prefix");
        String directory = libElement.getAttribute("directory");
        NodeList filesContainer = libElement.getElementsByTagNameNS("*", "files");
        List<Entry> entries = new ArrayList<>(filesContainer.getLength());
        if (filesContainer.getLength() > 0) {
            Element filesElement = (Element) filesContainer.item(0);
            NodeList fileNodes = filesElement.getElementsByTagNameNS("*", "file");
            for (int j = 0; j < fileNodes.getLength(); j++) {
                Element fileElement = (Element) fileNodes.item(j);
                Entry e = new Entry(fileElement.getAttribute("name"),
                        fileElement.getAttribute("load")
                                .equalsIgnoreCase(Boolean.TRUE.toString()));
                entries.add(e);
            }
        }
        return new Library(prefix, name, directory, entries);
    }

    private static class Library {
        private final String prefix;
        private final String name;
        private final String directory;
        private final List<Entry> entries;

        public Library(String prefix, String name, String directory, List<Entry> entries) {
            this.prefix = prefix;
            this.name = name;
            this.directory = directory;
            this.entries = entries;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getName() {
            return name;
        }

        public String getDirectory() {
            return directory;
        }

        public List<Entry> getEntries() {
            return entries;
        }

    }

    private static class Entry {
        private final String name;
        private final boolean load;

        public Entry(String name, boolean load) {
            this.name = name;
            this.load = load;
        }

        public String getName() {
            return name;
        }

        public boolean shouldLoad() {
            return load;
        }

    }

}
