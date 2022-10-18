package eu.sakarah.tool.mapping.mappingSheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.sakarah.smooks.utils.TallystickFunctions;
import eu.sakarah.tool.mapping.excel.ExcelUtils;

public class GenerateMappingTransformation {

	protected static final Logger LOGGER = LoggerFactory.getLogger(GenerateMappingTransformation.class);

	public static final String MAPPING_FILE_NAME_XSLT = "transform.xsl";
	private static final String MAPPING_SHEET_NAME_PREFIX = "Correspondance";
	private static final String MAPPING_SHEET_NAME_PIVOT = "pivot";

	private static List<String> FUNCTIONS_SUPPORTED = new ArrayList<String>();
	static {
		for (Method method : TallystickFunctions.class.getDeclaredMethods()) {
			if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
				FUNCTIONS_SUPPORTED.add(method.getName());
			}
		}
	}

	private static final Namespace XSLT_NAME_SPACE = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
	private static final Namespace DATE_NAME_SPACE = Namespace.getNamespace("date", "http://exslt.org/dates-and-times");
	private static final Namespace JAVA_NAME_SPACE = Namespace.getNamespace("java", "http://xml.apache.org/xalan/java");
	private static final Namespace TALLY_NAME_SPACE = Namespace.getNamespace("tally", "xalan://eu.sakarah.smooks.utils.TallystickFunctions");

	public static boolean isInputMapping(XSSFSheet sheet) {

		return isInputMapping(sheet.getSheetName());
	}

	public static boolean isInputMapping(String sheetName) {

		return sheetName.startsWith(MAPPING_SHEET_NAME_PREFIX) && StringUtils.containsIgnoreCase(sheetName, MAPPING_SHEET_NAME_PIVOT);
	}

	public static boolean isOutputMapping(XSSFSheet sheet) {

		return isOutputMapping(sheet.getSheetName());
	}

	public static boolean isOutputMapping(String sheetName) {

		return sheetName.startsWith(MAPPING_SHEET_NAME_PREFIX) && !StringUtils.containsIgnoreCase(sheetName, MAPPING_SHEET_NAME_PIVOT);
	}

	public static String generateMappingDirectoryName(String sheetName) {

		return sheetName.replaceAll(" ", "_");
	}

	public static String generate(XSSFSheet sheet, File mappingDirectory) throws Exception {

		boolean isInputMapping = isInputMapping(sheet);

		// Read Excel file and generate the corresponding tree.
		Map<ColumnHeader, Integer> columnsConfiguration = null;
		RowContent rootNode = new RootContent();
		RowContent lastNode = rootNode;
		int rowIndex = 0;
		while (sheet.getRow(rowIndex) != null) {

			XSSFRow row = sheet.getRow(rowIndex);
			if (columnsConfiguration == null && isHeaderRow(row)) {
				LOGGER.info("Entête de mapping trouvé à la ligne " + (row.getRowNum()+1));
				columnsConfiguration = readHeaderRow(row);
			} else if (columnsConfiguration != null) {
				RowContent node = readRow(lastNode, columnsConfiguration, row);
				if (node != null) {
					lastNode = node;
				}
			}

			rowIndex++;
		}

		if (rootNode == null) {
			throw new RuntimeException("Aucune ligne trouvée dans ce mapping. Chaque mapping doit avoir au moins une ligne active.");
		}

		RowContent first = rootNode.getChildren().get(0);

		// Generate the XLST file.
		String rootXPath = first.getInputField().replaceAll("^/", "");
		first.setInputField(null);

		// Ajustement de champs inputField à mapper pour les noeuds et attributs (à l'intérieur des itération, supprime le préfix de cette itération).
		adjustTreeMappings(first, rootXPath);

		// Generate the mapping output directory.
		mappingDirectory.mkdir();

		File xslFile = new File(mappingDirectory, MAPPING_FILE_NAME_XSLT);
		OutputStream xslOutputStream = new FileOutputStream(xslFile);
		generateXsltFile(rootXPath, isInputMapping, rootNode, xslOutputStream);

		return rootXPath;
	}

	private static boolean isHeaderRow(XSSFRow row) {

		int columnIndex = 0;
		while (row.getCell(columnIndex) != null) {
			String cellContent = ExcelUtils.readCell(row, columnIndex);
			if (ColumnHeader.fromHeaderText(cellContent) != null) {
				return true;
			}
			columnIndex++;
		}
		return false;
	}

	private static Map<ColumnHeader, Integer> readHeaderRow(XSSFRow row) throws Exception {

		Map<ColumnHeader, Integer> columnsConfiguration = new HashMap<ColumnHeader, Integer>();
		int columnIndex = 0;
		while (row.getCell(columnIndex) != null) {
			String cellContent = ExcelUtils.readCell(row, columnIndex);
			ColumnHeader mappingHeaderColumn = ColumnHeader.fromHeaderText(cellContent);
			if (mappingHeaderColumn != null) {
				columnsConfiguration.put(mappingHeaderColumn, columnIndex);
			}
			columnIndex++;
		}

		for (ColumnHeader mappingColumnHeader : ColumnHeader.values()) {
			if (!columnsConfiguration.containsKey(mappingColumnHeader)) {
				throw new RuntimeException("Toutes les colonnes n'ont pû être trouvées dans l'entête à la ligne " + (row.getRowNum()+1) + ". La liste des colonnes attendues est " + StringUtils.join(ColumnHeader.getHeaderTexts(), ", ") + ".");
			}
		}

		return columnsConfiguration;
	}

	private static RowContent readRow(RowContent lastNode, Map<ColumnHeader, Integer> columnsConfiguration, XSSFRow row) {

		if (ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.USE)) != null) {

			RowContent rowContent = new RowContent(row);
			int nodeOutputLevel = 0;

			// Input value.
			String inputValue = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.INPUT_VALUE));
			if (StringUtils.startsWith(inputValue, "\"") && StringUtils.endsWith(inputValue, "\"")) {
				inputValue = inputValue.substring(1, inputValue.length()-1);
				rowContent.setInputConstant(inputValue);
			} else {
				rowContent.setInputField(inputValue);
			}

			// Iteration condition.
			String iterationValue = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.ITERATE));
			rowContent.setIterate(iterationValue);

			// Display condition.
			String displayConditionValue = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.DISPLAY_CONDITION));
			rowContent.setDisplayCondition(displayConditionValue);

			// Output value.
			String outputValue = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.OUTPUT_VALUE));
			if (outputValue == null) {
				throw new RuntimeException("Pas d'output value définie sur cette ligne, ce qui n'a aucun sens.");
			}
			outputValue = outputValue.replaceAll("[<> ]", "");
			rowContent.setAttribute(outputValue.startsWith("@"));
			if (rowContent.isAttribute()) {
				outputValue = outputValue.replaceFirst("@", "");
			} else {
				nodeOutputLevel = outputValue.replaceAll("^([+]*)[^+]*", "$1").length();
				outputValue = outputValue.replaceAll("^[+]*", "");
			}
			boolean isAlternative = outputValue.startsWith("|");
			if (isAlternative) {
				outputValue = outputValue.replaceFirst("\\|[+]*", "");
			}
			rowContent.setOutputField(outputValue);

			// Output format.
			String outputFormat = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.OUTPUT_FORMAT));
			if (FormatType.NUMBER.getText().equals(outputFormat)) {
				rowContent.setOutputFormatType(FormatType.NUMBER);
			} else {
				rowContent.setOutputFormatType(FormatType.STRING);
				if (outputFormat != null) {
					if (outputFormat.startsWith(FormatType.STRING.getText())) {
						outputFormat = outputFormat.replaceAll(FormatType.STRING.getText(), "").trim();
					}
					if (NumberUtils.toInt(outputFormat) != 0) {
						rowContent.setOutputMaxLength(NumberUtils.toInt(outputFormat));
					} else {
						rowContent.setOutputFormat(outputFormat);
					}
				}
			}

			// Insert the row in the tree.
			if (isAlternative) {
				lastNode.setAlternative(rowContent);
				// return lastNode;
			}
			else if (rowContent.isAttribute()) {
				if (lastNode == null) {
					throw new RuntimeException("Le premier élément du mapping (ligne " + row.getRowNum() + ") a été identifié comme un attribut (commence par '@'). Le premier élément ne peut être qu'un noeud.");
				}
				// TODO
				if (outputValue.startsWith("xmlns:")) {
					String prefix = outputValue.substring(6);
					String uri = rowContent.getInputConstant();
					lastNode.addNamespace(prefix, uri);
					/*if (lastNode instanceof RootContent) {
						((RootContent) lastNode).addNamespace(prefix, uri);
					} else {
						throw new RuntimeException("xmlns");
					}*/
				} else {
					lastNode.addAttribute(rowContent);
				}
				return lastNode;
			} else if (lastNode == null) {
				if (nodeOutputLevel > 0) {
					throw new RuntimeException("Le premier élément du mapping (ligne " + row.getRowNum() + ") a été identifié comme un sous-noeud (commence par au moins un '+'). Le premier élément ne peut être qu'un noeud.");
				}
			} else {
				if (lastNode.getOutputLevel() == nodeOutputLevel) {
					lastNode.getParent().addChild(rowContent);
				} else if (lastNode.getOutputLevel() + 1 == nodeOutputLevel) {
					lastNode.addChild(rowContent);
				} else if (lastNode.getOutputLevel() > nodeOutputLevel) {
					while (lastNode.getOutputLevel() > nodeOutputLevel) {
						lastNode = lastNode.getParent();
					}
					lastNode.getParent().addChild(rowContent);
				} else {
					throw new RuntimeException("Le noeud courant ne peut être inséré dans l'arborescence (ligne " + row.getRowNum() + "). Niveau de noeud trouvé : " + nodeOutputLevel + ". Niveau du noeud précédent : " + lastNode.getOutputLevel() + ".");
				}
			}
			return rowContent;
		}

		return null;
	}

	private static void adjustTreeMappings(RowContent rowContent, String currentIterationRoot) {

		if (rowContent.getIterate() != null) {
			currentIterationRoot = rowContent.getIterate();
		}

		if (currentIterationRoot != null) {
			rowContent.setInputField(adjust(rowContent.getInputField(), currentIterationRoot));
			rowContent.setDisplayCondition(adjust(rowContent.getDisplayCondition(), currentIterationRoot));
		}

		if (!rowContent.isAttribute()) {
			for (RowContent rowAttribute : rowContent.getAttributes()) {
				adjustTreeMappings(rowAttribute, currentIterationRoot);
			}
			for (RowContent rowChild : rowContent.getChildren()) {
				adjustTreeMappings(rowChild, currentIterationRoot);
			}
		}
	}

	private static String adjust(String previousValue, String currentIterationRoot) {

		String value = previousValue;
		if (value != null) {
			value = value.replaceAll(currentIterationRoot + "/", "");
			if (!value.equals(previousValue)) {
				LOGGER.info("Adjust value from " + previousValue + " (" + currentIterationRoot + ") to " + value);
			}
		}


		/*if (previousValue != null && previousValue.matches("^.*" + currentIterationRoot + "\\W.*")) {
			value = previousValue.replaceAll("/?\\b" + currentIterationRoot + "(\\W)", "$1");
			value = value.replaceFirst("^/", "");
			LOGGER.info("Adjust value from " + previousValue + " (" + currentIterationRoot + ") to " + value);
		}*/
		return value;
	}

	private static void generateXsltFile(String rootXPath, boolean isInputMapping, RowContent rootRowContent, OutputStream outputStream) {
		Document document = new Document();

		Element stylesheet = new Element("stylesheet", XSLT_NAME_SPACE);
		stylesheet.addNamespaceDeclaration(XSLT_NAME_SPACE);
		stylesheet.addNamespaceDeclaration(DATE_NAME_SPACE);
		stylesheet.addNamespaceDeclaration(JAVA_NAME_SPACE);
		stylesheet.addNamespaceDeclaration(TALLY_NAME_SPACE);
		stylesheet.setAttribute("version", "2.0");
		stylesheet.setAttribute("extension-element-prefixes", DATE_NAME_SPACE.getPrefix());
		document.setRootElement(stylesheet);

		Element output = new Element("output", XSLT_NAME_SPACE);
		output.setAttribute("method", "xml");
		output.setAttribute("version", "1.0");
		output.setAttribute("encoding", "UTF-8");
		output.setAttribute("indent", "yes");
		// output.setAttribute("content-handler", XmlSerializer.class.getCanonicalName(), xalanNameSpace);
		stylesheet.addContent(output);

		Element baseTemplate = null;
		Element invoiceTemplate = null;
		Element rootTemplate = null;
		if (isInputMapping) {

			baseTemplate = new Element("template", XSLT_NAME_SPACE);
			baseTemplate.setAttribute("match", "/" + rootXPath);

			Element rootVariable = new Element("variable", XSLT_NAME_SPACE);
			rootVariable.setAttribute("name", "rootNode");
			rootVariable.setAttribute("select", ".");
			stylesheet.addContent(rootVariable);

			stylesheet.addContent(baseTemplate);

		} else {
			Element rootVariable = new Element("variable", XSLT_NAME_SPACE);
			rootVariable.setAttribute("name", "rootNode");
			rootVariable.setAttribute("select", "Invoice/Customer_File");
			stylesheet.addContent(rootVariable);

			rootTemplate = new Element("template", XSLT_NAME_SPACE);
			rootTemplate.setAttribute("match", "/");
			stylesheet.addContent(rootTemplate);
			Element applyBaseTemplate = new Element("apply-templates", XSLT_NAME_SPACE);
			applyBaseTemplate.setAttribute("select", "Invoice");
			rootTemplate.addContent(applyBaseTemplate);

			invoiceTemplate = new Element("template", XSLT_NAME_SPACE);
			invoiceTemplate.setAttribute("match", "Invoice");
			stylesheet.addContent(invoiceTemplate);
			Element applyInvoiceTemplate = new Element("apply-templates", XSLT_NAME_SPACE);
			applyInvoiceTemplate.setAttribute("select", "Customer_File");
			invoiceTemplate.addContent(applyInvoiceTemplate);

			baseTemplate = new Element("template", XSLT_NAME_SPACE);
			baseTemplate.setAttribute("match", "Customer_File");

			// <xsl:variable name="rootNode" select="."/>

			stylesheet.addContent(baseTemplate);

		}

		Element mappingElement = generateXsltElement(stylesheet, rootRowContent);

		if (isInputMapping) {

			Element element = new Element("element", XSLT_NAME_SPACE);
			element.setAttribute("name", "Customer_File");
			mappingElement.addContent(element);

			Element copyOf = new Element("copy-of", XSLT_NAME_SPACE);
			copyOf.setAttribute("select", "./*");
			element.addContent(copyOf);
		}

		baseTemplate.addContent(mappingElement);

		Format format = Format.getPrettyFormat();
		format.setEncoding("UTF-8");
		format.setIndent("\t");
		XMLOutputter xmlOutput = new XMLOutputter(format);
		try {
			xmlOutput.output(document, outputStream);
		} catch (IOException e) {
			throw new RuntimeException("Can't write the output XSLT file.", e);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	private static int templateNum = 1;

	private static Element generateXsltElement(Element stylesheet, RowContent rowContent) {

		Element element = null;

		if (rowContent.getAlternative() != null) {
			Element choose = new Element("choose", XSLT_NAME_SPACE);
			String templateName = "tmpl" + templateNum;
			++templateNum;

			Element tmpl = new Element("template", XSLT_NAME_SPACE);
			tmpl.setAttribute("name", templateName);
			stylesheet.addContent(tmpl);

			for (RowContent row = rowContent; row != null; row = row.getAlternative()) {
				Element alt;
				if (rowContent.getDisplayCondition() != null) {
					alt = new Element("when", XSLT_NAME_SPACE);
					alt.setAttribute("test", filterSakarahFunctions(row.getDisplayCondition()));
				} else {
					alt = new Element("otherwise", XSLT_NAME_SPACE);
				}
				Element altElem =  new Element("element", XSLT_NAME_SPACE);
				altElem.setAttribute("name", row.getOutputField());
				for (RowContent rowAttribute : row.getAttributes()) {
					Element attribute = generateXsltElement(stylesheet, rowAttribute);
					addAttribute(altElem, attribute);
				}

				Element call = new Element("call-template", XSLT_NAME_SPACE);
				call.setAttribute("name", templateName);
				altElem.addContent(call);

				if (row.getIterate() != null) {
					Element forElem = new Element("for-each", XSLT_NAME_SPACE);
					forElem.setAttribute("select", row.getIterate());
					forElem.addContent(altElem);
					altElem = forElem;
				}
				alt.addContent(altElem);

				choose.addContent(alt);
			}

			/*
			if (rowContent.getIterate() != null) {
				Element forElem = new Element("for-each", XSLT_NAME_SPACE);
				forElem.setAttribute("select", rowContent.getIterate());
				tmpl.addContent(forElem);
				tmpl = forElem;
			}
			*/
			addDummyAttrs(rowContent, tmpl);
			for (RowContent rowChild : rowContent.getChildren()) {
				Element child = generateXsltElement(stylesheet, rowChild);
				tmpl.addContent(child);
			}

			return choose;
		}

		if (rowContent instanceof RootContent) {
			RootContent rootContent = (RootContent) rowContent;
			for (Namespace ns : rootContent.getNamespaces()) {
				stylesheet.addNamespaceDeclaration(ns);
			}
			return generateXsltElement(stylesheet, rootContent.getChildren().get(0));
		}

		if (!rowContent.isAttribute()) {
			element = new Element("element", XSLT_NAME_SPACE);
			addDummyAttrs(rowContent, element);
			element.setAttribute("name", rowContent.getOutputField());
			for (RowContent rowAttribute : rowContent.getAttributes()) {
				Element attribute = generateXsltElement(stylesheet, rowAttribute);
				addAttribute(element, attribute);
			}
			for (RowContent rowChild : rowContent.getChildren()) {
				Element child = generateXsltElement(stylesheet, rowChild);
				element.addContent(child);
			}
		} else {
			element = new Element("attribute", XSLT_NAME_SPACE);
			element.setAttribute("name", rowContent.getOutputField());
		}

		if (rowContent.getInputConstant() != null) {
			Element text = new Element("text", XSLT_NAME_SPACE);
			text.addContent(rowContent.getInputConstant());
			element.addContent(text);
		} else if (rowContent.getInputField() != null) {

			String inputField = filterSakarahFunctions(rowContent.getInputField());
			inputField = inputField.replaceAll("^/", "\\$rootNode/");

			// Gestion du format.
			if (FormatType.NUMBER.equals(rowContent.getOutputFormatType())) {
				inputField = "format-number(" + inputField + ", '#0.###')";
			} else if (FormatType.STRING.equals(rowContent.getOutputFormatType())) {
				if (rowContent.getOutputMaxLength() != null) {
					inputField = "substring(" + inputField + ", 1, " + rowContent.getOutputMaxLength() + ")";
				}
			}

			Element valueOf = new Element("value-of", XSLT_NAME_SPACE);
			valueOf.setAttribute("select", inputField);
			element.addContent(valueOf);
		}

		Element elementToReturn = element;
		if (rowContent.getDisplayCondition() != null) {
			Element condition = null;
			condition = new Element("if", XSLT_NAME_SPACE);
			condition.setAttribute("test", filterSakarahFunctions(rowContent.getDisplayCondition()));
			condition.addContent(elementToReturn);
			elementToReturn = condition;
		}
		if (!rowContent.isAttribute()) {
			Element forEach = null;
			if (rowContent.getIterate() != null) {
				forEach = new Element("for-each", XSLT_NAME_SPACE);
				forEach.setAttribute("select", rowContent.getIterate());
				forEach.addContent(elementToReturn);
				elementToReturn = forEach;
			}
		}

		return elementToReturn;
	}

	private static void addAttribute(Element element, Element attribute) {
		Attribute name = attribute.getAttribute("name");
		if (name != null && name.getValue().equals("xmlns")) {
			element.setAttribute("namespace", attribute.getValue());
		} else {
			element.addContent(attribute);
		}
	}

	private static void addDummyAttrs(RowContent rowContent, Element element) {
		if (rowContent.getOutputLevel() == 0) {
			Stream.iterate(rowContent, RowContent::getParent)
					.filter(RootContent.class::isInstance)
					.map(RootContent.class::cast)
					.findFirst()
					.ifPresent(root ->
							root.getNamespaces()
									.stream()
									.map(Namespace::getPrefix)
									.map(prefix -> {
										Element attr = new Element("attribute", XSLT_NAME_SPACE);
										attr.setAttribute("name", prefix + ':' + XmlSerializer.DUMMY_ATT);
										return attr;
									})
									.forEach(element::addContent));
		}
	}

	private static String filterSakarahFunctions(String source) {

		StringBuilder builder = new StringBuilder();
		Pattern pattern = Pattern.compile("(\\w*)\\(");
		Matcher matcher = pattern.matcher(source);
		while (matcher.find()) {
			String function = matcher.group(1);
			builder.append(source.substring(0, matcher.start()));
			if (FUNCTIONS_SUPPORTED.contains(function)) {
//				builder.append("java:");
//				builder.append(TallystickFunctions.class.getName());
//				builder.append(".");
				builder.append("tally:");
			}
			builder.append(function);
			builder.append("(");
			source = source.substring(matcher.end(), source.length());
			matcher = pattern.matcher(source);
		}
		builder.append(source);
		return builder.toString();
	}
}
