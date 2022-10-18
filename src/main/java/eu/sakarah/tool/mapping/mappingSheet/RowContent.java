package eu.sakarah.tool.mapping.mappingSheet;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RowContent {

	protected static final Logger LOGGER = LoggerFactory.getLogger(RowContent.class);

	private RowContent parent = null;
	private List<RowContent> attributes = new ArrayList<RowContent>();
	private List<RowContent> children = new ArrayList<RowContent>();
	private RowContent alternative;
	private int outputLevel = 0;

	private XSSFRow row = null;
	private String outputField = null;
	private boolean attribute = false;
	private FormatType outputFormatType = null;
	private String outputFormat = null;
	private Integer outputMaxLength = null;
	private String iterate = null;
	private String displayCondition = null;
	private String inputField = null;
	private String inputConstant = null;

	public RowContent(XSSFRow row) {
		this.row = row;
	}

	public XSSFRow getRow() {
		return row;
	}

	public void setRow(XSSFRow row) {
		this.row = row;
	}

	public RowContent getParent() {
		return parent;
	}

	public void setParent(RowContent parent) {
		this.parent = parent;
	}

	public List<RowContent> getChildren() {
		return (alternative != null ? alternative : this).children;
	}

	public void addChild(RowContent child) {
		child.setParent(this);
		child.setOutputLevel(getOutputLevel() + 1);
		children.add(child);
		child.displayRowContent();
	}

	public List<RowContent> getAttributes() {
		return attributes;
	}

	public void addAttribute(RowContent attribute) {
		attribute.setParent(this);
		attribute.setOutputLevel(outputLevel + 1);
		attributes.add(attribute);
		attribute.displayRowContent();
	}

	public void addNamespace(Namespace ns) {
		getParent().addNamespace(ns);
	}

	public void addNamespace(String prefix, String uri) {
		addNamespace(Namespace.getNamespace(prefix, uri));
	}

	public RowContent getAlternative() {
		return alternative;
	}

	public void setAlternative(RowContent alternative) {
		alternative.setParent(parent);
		alternative.setOutputLevel(outputLevel);
		this.alternative = alternative;
	}

	public String getOutputField() {
		return outputField;
	}

	public void setOutputField(String outputField) {
		this.outputField = outputField;
	}

	public int getOutputLevel() {
		return outputLevel;
	}

	private void setOutputLevel(int outputLevel) {
		this.outputLevel = outputLevel;
	}

	public boolean isAttribute() {
		return attribute;
	}

	public void setAttribute(boolean attribute) {
		this.attribute = attribute;
	}

	public FormatType getOutputFormatType() {
		return outputFormatType;
	}

	public void setOutputFormatType(FormatType outputFormatType) {
		this.outputFormatType = outputFormatType;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public Integer getOutputMaxLength() {
		return outputMaxLength;
	}

	public void setOutputMaxLength(Integer outputMaxLength) {
		this.outputMaxLength = outputMaxLength;
	}

	public String getIterate() {
		return iterate;
	}

	public void setIterate(String iterate) {
		this.iterate = iterate;
	}

	public String getDisplayCondition() {
		return displayCondition;
	}

	public void setDisplayCondition(String displayCondition) {
		this.displayCondition = displayCondition;
	}

	public String getInputField() {
		return inputField;
	}

	public void setInputField(String inputField) {
		this.inputField = inputField;
	}

	public String getInputConstant() {
		return inputConstant;
	}

	public void setInputConstant(String inputConstant) {
		this.inputConstant = inputConstant;
	}

	public void displayTreeRowContent() {

		displayRowContent();
		for (RowContent attribute : attributes) {
			attribute.displayRowContent();
		}
		for (RowContent child : children) {
			child.displayTreeRowContent();
		}
	}

	public void displayRowContent() {

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < outputLevel; i++) {
			builder.append("    ");
		}
		builder.append(this);
		LOGGER.info(builder.toString());
	}

	@Override
	public String toString() {

		String prefix = ", ";
		StringBuilder builder = new StringBuilder("RowContent[");
		if (attribute) {
			builder.append("attribute").append(prefix);
		}
		builder.append("output: '").append(outputField).append("'");
		if (iterate != null) {
			builder.append(prefix).append("iterate: '").append(iterate).append("'");
		}
		if (displayCondition != null) {
			builder.append(prefix).append("display condition: '").append(displayCondition).append("'");
		}
		if (inputField != null) {
			builder.append(prefix).append("input field: '").append(inputField).append("'");
			builder.append(prefix).append("output format type: '").append(outputFormatType.name()).append("'");
			if (outputFormat != null) {
				builder.append(prefix).append("output format: '").append(outputFormat).append("'");
			}
			if (outputMaxLength != null) {
				builder.append(prefix).append("output max length: '").append(outputMaxLength).append("'");
			}
		} else if (inputConstant != null) {
			builder.append(prefix).append("input constant: '").append(inputConstant).append("'");
		}
		if (row != null) {
			builder.append(", row number: ").append(row.getRowNum()+1);
		}
		builder.append("]");
		builder.append("]");
		return builder.toString();
	}

}
