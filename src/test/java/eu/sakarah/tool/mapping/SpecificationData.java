package eu.sakarah.tool.mapping;

import java.util.Arrays;
import java.util.List;

public class SpecificationData {

	private String excelFile = null;
	private String targetWorkingDirectory = null;
	private List<TestData> tests = null;

	public SpecificationData(String excelFile, String targetWorkingDirectory, TestData... tests) {

		this.excelFile = excelFile;
		this.targetWorkingDirectory = targetWorkingDirectory;
		this.tests = Arrays.asList(tests);
	}

	public String getExcelFile() {
		return excelFile;
	}

	public String getTargetWorkingDirectory() {
		return targetWorkingDirectory;
	}

	public List<TestData> getTests() {
		return tests;
	}
}
