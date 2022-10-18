package eu.sakarah.tool.mapping.inputFormatSheet;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RowContent {

	protected static final Logger LOGGER = LoggerFactory.getLogger(RowContent.class);

	public enum Type {ROOT, GROUP, SEGMENT, LEVEL1, LEVEL2};

	private RowContent parent = null;
	private List<RowContent> children = new ArrayList<RowContent>();
	private int outputLevel = 0;

	private XSSFRow row = null;
	private Type type = null;
	private String name = null;
	private boolean truncable;

	public static RowContent createRootRowContent() {

		RowContent rootRowContent = new RowContent(null, Type.ROOT, null, false);
		rootRowContent.displayRowContent();
		return rootRowContent;
	}

	public RowContent(XSSFRow row, Type type, String name) {
		this.row = row;
		this.type = type;
		this.name = name;
		this.truncable = false;
	}
	
	public RowContent(XSSFRow row, Type type, String name, boolean truncable) {
		this.row = row;
		this.type = type;
		this.name = name;
		this.truncable = truncable;
	}

	public RowContent getParent() {
		return parent;
	}

	public void setParent(RowContent parent) {
		this.parent = parent;
	}

	public List<RowContent> getChildren() {
		return children;
	}

	public void setChildren(List<RowContent> children) {
		this.children = children;
	}

	public void addChild(RowContent child) {
		child.setParent(this);
		child.setOutputLevel(outputLevel + 1);
		children.add(child);
		child.displayRowContent();
	}

	public int getOutputLevel() {
		return outputLevel;
	}

	public void setOutputLevel(int outputLevel) {
		this.outputLevel = outputLevel;
	}

	public XSSFRow getRow() {
		return row;
	}

	public void setRow(XSSFRow row) {
		this.row = row;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isTruncable() {
		return truncable;
	}

	public void setTruncable(boolean truncable) {
		this.truncable = truncable;
	}

	public void displayTreeRowContent() {

		displayRowContent();
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

		StringBuilder builder = new StringBuilder();
		builder.append("RowContent[type: '").append(type.name()).append("', name: '").append(name).append("'");
		if (row != null) {
			builder.append(", row number: ").append(row.getRowNum()+1);
		}
		builder.append("]");
		return builder.toString();
	}
}
