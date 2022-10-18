package eu.sakarah.tool.mapping.inputFormatSheet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.sakarah.tool.mapping.excel.ExcelUtils;
import eu.sakarah.tool.mapping.inputFormatSheet.RowContent.Type;

public abstract class AbstractGenerateSegmentedInputFormat {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractGenerateSegmentedInputFormat.class);

	public abstract List<String> getSeparators();

	private static final String FORMAT_FILE_NAME_XML = "from-customer-file-to-intermediary-xml.xml";

	public File generate(XSSFSheet sheet, File mappingDirectory) throws Exception {

		// Read Excel sheet and generate the corresponding tree.
		Map<ColumnHeader, Integer> columnsConfiguration = null;
		RowContent rootRowContent = RowContent.createRootRowContent();
		RowContent lastRowContent = rootRowContent;

		int rowIndex = 0;
		while (sheet.getRow(rowIndex) != null) {

			XSSFRow row = sheet.getRow(rowIndex);
			if (columnsConfiguration == null && isHeaderRow(row)) {
				columnsConfiguration = readHeaderRow(row);
			} else if (columnsConfiguration != null) {
				RowContent newLastRowContent = readRow(lastRowContent, columnsConfiguration, row);
				if (newLastRowContent == null) {
					break;
				}
				lastRowContent = newLastRowContent;
			}
			rowIndex++;
		}

		if (lastRowContent == null) {
			throw new RuntimeException("Aucune ligne trouvée dans le format d'entrée. Chaque format doit avoir au moins une ligne active.");
		}

		// Generate the mapping output directory.
		mappingDirectory.mkdir();

		File xmlFile = new File(mappingDirectory, FORMAT_FILE_NAME_XML);
		OutputStream outputStream = new FileOutputStream(xmlFile);
		generateMappingFile(rootRowContent, outputStream);
		return xmlFile;
	}

	private boolean isHeaderRow(XSSFRow row) {

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

	private Map<ColumnHeader, Integer> readHeaderRow(XSSFRow row) {

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

	private RowContent readRow(RowContent lastRowContent, Map<ColumnHeader, Integer> columnsConfiguration, XSSFRow row) {

		String allGroupNames = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.GROUP_NAME));
		String segmentName = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.SEGMENT_NAME));
		String level1Name = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.LEVEL1_NAME));
		String level2Name = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.LEVEL2_NAME));
		String truncable = ExcelUtils.readCell(row, columnsConfiguration.get(ColumnHeader.TRUNCATABLE));

		if (segmentName != null) {

			// Split group names.
			List<String> groupNames = new ArrayList<String>();
			if (allGroupNames != null) {
				groupNames = Arrays.asList(allGroupNames.split("/"));
			}

			// Create all groups.
			List<RowContent> groupAncestors = getGroupAncestors(lastRowContent);
			RowContent parent = getAncestor(lastRowContent, Type.ROOT);
			for (String groupName : groupNames) {
				if (groupAncestors.isEmpty()) {
					// The doesn't exist.
					RowContent group = new RowContent(row, Type.GROUP, groupName);
					parent.addChild(group);
					parent = group;
				} else if (groupAncestors.get(0).getName().equals(groupName)) {
					// The group exists.
					parent = groupAncestors.get(0);
					groupAncestors.remove(0);
				} else {
					// The group doesn't exist.
					groupAncestors.clear();
					RowContent group = new RowContent(row, Type.GROUP, groupName);
					parent.addChild(group);
					parent = group;
				}
			}

			// Add segment.
			RowContent segment = new RowContent(row, Type.SEGMENT, segmentName,(truncable != null));
			parent.addChild(segment);

			// Vu que j'en ai marre de séparer les segments et les niveau 1 sur des lignes différentes, on va dire qu'on peut mettre les deux infos sur une seul ligne hein ...
			if (level1Name != null) {
				lastRowContent = segment;
			} else {
				return segment;
			}
		}
		if (level1Name != null) {
			RowContent level1 = new RowContent(row, Type.LEVEL1, level1Name,(truncable != null));
			RowContent segmentAncestor = getAncestor(lastRowContent, Type.SEGMENT);
			if (segmentAncestor == null) {
				throw new RuntimeException("Ligne " + (row.getRowNum()+1) + " : Un niveau 1 ne peut être inséré qu'après un segment, un autre niveau 1 ou un niveau 2.");
			}
			segmentAncestor.addChild(level1);
			return level1;
		}

		if (level2Name != null) {
			RowContent level2 = new RowContent(row, Type.LEVEL2, level2Name);
			RowContent level1Ancestor = getAncestor(lastRowContent, Type.LEVEL1);
			if (level1Ancestor == null) {
				throw new RuntimeException("Ligne " + (row.getRowNum()+1) + " : Un niveau 2 ne peut être inséré qu'après un niveau 1 ou un autre niveau 2.");
			}
			level1Ancestor.addChild(level2);
			return level2;
		}

		LOGGER.info("Ligne " + (row.getRowNum()+1) + " : Ligne dont le contenu de peut être interprêté. Toutes les lignes suivantes ne seront pas lues !");
		return null;
	}

	private RowContent getAncestor(RowContent rowContent, Type type) {

		if (rowContent == null) {
			return null;
		}
		if (rowContent.getType().equals(type)) {
			return rowContent;
		}
		return getAncestor(rowContent.getParent(), type);
	}

	private List<RowContent> getGroupAncestors(RowContent rowContent) {

		List<RowContent> groups = new ArrayList<RowContent>();
		while (rowContent != null) {
			if (Type.GROUP.equals(rowContent.getType())) {
				groups.add(0, rowContent);
			}
			rowContent = rowContent.getParent();
		}
		return groups;
	}

	private void generateMappingFile(RowContent rootRowContent, OutputStream outputStream) {

		Document document = new Document();
		Namespace smooksEdiNameSpace = Namespace.getNamespace("medi", "http://www.milyn.org/schema/edi-message-mapping-1.1.xsd");

		Element ediRoot = new Element("edimap", smooksEdiNameSpace);
		ediRoot.addNamespaceDeclaration(smooksEdiNameSpace);
		document.setRootElement(ediRoot);

		Element description = new Element("description", smooksEdiNameSpace);
		description.setAttribute("name", "Invoice Pivot");
		description.setAttribute("version", "1.0");
		ediRoot.addContent(description);

		List<String> separators = getSeparators();
		Element delimiters = new Element("delimiters", smooksEdiNameSpace);
		delimiters.setAttribute("segment", separators.size() > 0 ? separators.get(0) : "~");
		delimiters.setAttribute("field", separators.size() > 1 ? separators.get(1) : "~");
		delimiters.setAttribute("component", separators.size() > 2 ? separators.get(2) : "~");
		delimiters.setAttribute("sub-component", separators.size() > 3 ? separators.get(3) : "~");
		ediRoot.addContent(delimiters);

		Element element = generateMappingNodes(smooksEdiNameSpace, rootRowContent, 0);
		ediRoot.addContent(element);

		Format format = Format.getPrettyFormat();
		format.setEncoding("UTF-8");
		format.setIndent("\t");
		XMLOutputter xmlOutput = new XMLOutputter(format);
		try {
			xmlOutput.output(document, outputStream);
		} catch (IOException e) {
			throw new RuntimeException("Can't write the format file.", e);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	private Element generateMappingNodes(Namespace smooksEdiNameSpace, RowContent rowContent, int position) {

		Element currentElement = null;
		switch (rowContent.getType()) {
		case ROOT:
			currentElement = new Element("segments", smooksEdiNameSpace);
			currentElement.setAttribute("xmltag", "Facture");
			break;
		case GROUP:
			currentElement = new Element("segmentGroup", smooksEdiNameSpace);
			currentElement.setAttribute("xmltag", rowContent.getName());
			currentElement.setAttribute("minOccurs", "0");
			currentElement.setAttribute("maxOccurs", "-1");
			break;
		case SEGMENT:
			currentElement = new Element("segment", smooksEdiNameSpace);
			currentElement.setAttribute("segcode", rowContent.getName());
			currentElement.setAttribute("xmltag", rowContent.getName());
			if (rowContent.isTruncable()){
				currentElement.setAttribute("truncatable", "true");
			}
			// With the D96A format, all first segments of a group/the root node are mandatory and restricted to one occurrence.
			if (position == 0 && Type.GROUP.equals(rowContent.getParent().getType())) {
				currentElement.setAttribute("minOccurs", "1");
				currentElement.setAttribute("maxOccurs", "1");
			} else {
				currentElement.setAttribute("minOccurs", "0");
				currentElement.setAttribute("maxOccurs", "-1");
			}
			break;
		case LEVEL1:
			currentElement = new Element("field", smooksEdiNameSpace);
			currentElement.setAttribute("xmltag", rowContent.getName());
			if (rowContent.isTruncable()){
				currentElement.setAttribute("truncatable", "true");
			}
			break;
		case LEVEL2:
			currentElement = new Element("component", smooksEdiNameSpace);
			currentElement.setAttribute("xmltag", rowContent.getName());
			break;
		}

		for (int childPosition = 0; childPosition < rowContent.getChildren().size(); childPosition++) {
			RowContent child = rowContent.getChildren().get(childPosition);
			Element element = generateMappingNodes(smooksEdiNameSpace, child, childPosition);
			currentElement.addContent(element);
		}

		return currentElement;
	}
}
