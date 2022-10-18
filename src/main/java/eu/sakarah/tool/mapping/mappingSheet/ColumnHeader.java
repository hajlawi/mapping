package eu.sakarah.tool.mapping.mappingSheet;

import java.util.ArrayList;
import java.util.List;

public enum ColumnHeader {

	USE("Utilisation"),
	ITERATE("Condition iteration"),
	DISPLAY_CONDITION("Condition affichage"),
	INPUT_VALUE("Nom du champ"),
	OUTPUT_VALUE("Nom Balise"),
	OUTPUT_FORMAT("Format");

	private String headerText = null;

	private ColumnHeader(String headerText) {
		this.headerText = headerText;
	}

	protected String getHeaderText() {
		return headerText;
	}

	public static ColumnHeader fromHeaderText(String headerText) {

		for (ColumnHeader mappingColumnHeader : ColumnHeader.values()) {
			if (mappingColumnHeader.getHeaderText().equals(headerText)) {
				return mappingColumnHeader;
			}
		}
		return null;
	}

	public static List<String> getHeaderTexts() {

		List<String> headerTexts = new ArrayList<String>();
		for (ColumnHeader mappingColumnHeader : ColumnHeader.values()) {
			headerTexts.add(mappingColumnHeader.getHeaderText());
		}
		return headerTexts;
	}
};
