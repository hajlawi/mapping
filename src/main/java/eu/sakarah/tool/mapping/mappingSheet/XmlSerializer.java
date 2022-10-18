package eu.sakarah.tool.mapping.mappingSheet;

import org.apache.xml.serializer.ToXMLStream;
import org.xml.sax.SAXException;

/**
 * XML Serializer Xalan.
 *
 * Supprime les attributs {@code __dummy__} insérés par la transformation pour forcer la déclaration des namespaces
 * à la racine du XML.
 */
public class XmlSerializer extends ToXMLStream
{
    public static final String DUMMY_ATT = "__dummy__";

    @Override
    public boolean addAttributeAlways(String uri, String localName, String rawName, String type, String value, boolean xslAttribute) {
        if (localName.equals(DUMMY_ATT)) {
            try {
                ensureAttributesNamespaceIsDeclared(uri, localName, rawName);
                return true;
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
        return super.addAttributeAlways(uri, localName, rawName, type, value, xslAttribute);
    }

}
