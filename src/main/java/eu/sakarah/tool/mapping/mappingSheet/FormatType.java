package eu.sakarah.tool.mapping.mappingSheet;

public enum FormatType {

	STRING("String"), NUMBER("Float");

	private String text = null;

	private FormatType(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
