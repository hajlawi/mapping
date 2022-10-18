package eu.sakarah.tool.mapping.inputFormatSheet;

import java.util.ArrayList;
import java.util.List;

public enum ColumnHeader {

	SEGMENT_NAME("Segment"),
	GROUP_NAME("Groupe"),
	LEVEL1_NAME("Niveau1"),
	LEVEL2_NAME("Niveau2"),
	TRUNCATABLE("Troncable");

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
