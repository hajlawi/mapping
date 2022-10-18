package eu.sakarah.tool.mapping;

public class TestData {

	private String mappingName = null;
	private String inputFilePath = null;
	private String outputFilePath = null;

	public TestData(String mappingName, String inputFilePath, String outputFilePath) {

		this.mappingName = mappingName;
		this.inputFilePath = inputFilePath;
		this.outputFilePath = outputFilePath;
	}

	public String getMappingName() {
		return mappingName;
	}

	public String getInputFilePath() {
		return inputFilePath;
	}

	public String getOutputFilePath() {
		return outputFilePath;
	}
}
