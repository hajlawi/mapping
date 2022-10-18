package eu.sakarah.tool.mapping.mappingSheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSmooksConfiguration {

	protected static final Logger LOGGER = LoggerFactory.getLogger(GenerateSmooksConfiguration.class);

	public static final String MAPPING_FILE_NAME_SMOOKS_CONFIG = "smooks-config.xml";

	public static void generate(String rootXPath, boolean isInputMapping, File intermediaryTransformation, File mappingDirectory) throws Exception {

		// Generate the Smooks file.
		File smooksFile = new File(mappingDirectory, MAPPING_FILE_NAME_SMOOKS_CONFIG);
		OutputStream smooksOutputStream = new FileOutputStream(smooksFile);

		Namespace smooksNameSpace = Namespace.getNamespace("http://www.milyn.org/xsd/smooks-1.1.xsd");
		Namespace xslNameSpace = Namespace.getNamespace("xsl", "http://www.milyn.org/xsd/smooks/xsl-1.1.xsd");
		Namespace ediNameSpace = Namespace.getNamespace("edi", "http://www.milyn.org/xsd/smooks/edi-1.1.xsd");

		Element smooksResourceList = new Element("smooks-resource-list", smooksNameSpace);
		smooksResourceList.addNamespaceDeclaration(smooksNameSpace);
		smooksResourceList.addNamespaceDeclaration(xslNameSpace);
		smooksResourceList.addNamespaceDeclaration(ediNameSpace);

		Element resourceConfig = new Element("resource-config", smooksNameSpace);
		Element resource = new Element("resource", smooksNameSpace);
		Element param = new Element("param", smooksNameSpace);
		param.setAttribute("name", "encoding");
		param.setText("ISO-8859-1");
		resourceConfig.setAttribute("selector", "/");
		resource.setText(Init.class.getCanonicalName());
		resourceConfig.addContent(resource);
		resourceConfig.addContent(param);
		smooksResourceList.addContent(resourceConfig);

		if (intermediaryTransformation != null) {
			Element xsl = new Element("reader", ediNameSpace);
			xsl.setAttribute("mappingModel", intermediaryTransformation.getName());
			smooksResourceList.addContent(xsl);
		}

		Element xsl = new Element("xsl", xslNameSpace);
		xsl.setAttribute("applyOnElement", isInputMapping ? rootXPath.replaceFirst("/.*", "") : "Invoice");
		// xsl.setAttribute("applyBefore", "true");
		smooksResourceList.addContent(xsl);

		Element template = new Element("template", xslNameSpace);
		File xsltFile = new File(smooksFile.getParentFile(), GenerateMappingTransformation.MAPPING_FILE_NAME_XSLT);
		template.addContent(xsltFile.toPath().toString().replaceAll("\\\\", "/"));
		xsl.addContent(template);

		Element use = new Element("use", xslNameSpace);
		xsl.addContent(use);

		Element bindTo = new Element("bindTo", xslNameSpace);
		bindTo.setAttribute("id", isInputMapping ? "document" : "invoice");
		use.addContent(bindTo);

		/* Element inline = new Element("inline", xslNameSpace);
		inline.setAttribute("directive", "replace");
		use.addContent(inline); */

		Document document = new Document();
		document.setRootElement(smooksResourceList);

		Format format = Format.getPrettyFormat();
		format.setEncoding("UTF-8");
		format.setIndent("\t");
		XMLOutputter xmlOutput = new XMLOutputter(format);
		try {
			xmlOutput.output(document, smooksOutputStream);
		} catch (IOException e) {
			throw new RuntimeException("Can't write the output Smooks configuration file.", e);
		} finally {
			IOUtils.closeQuietly(smooksOutputStream);
		}
	}
}
