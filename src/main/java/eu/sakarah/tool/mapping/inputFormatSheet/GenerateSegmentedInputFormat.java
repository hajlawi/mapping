package eu.sakarah.tool.mapping.inputFormatSheet;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateSegmentedInputFormat extends AbstractGenerateSegmentedInputFormat {

	protected static final Logger LOGGER = LoggerFactory.getLogger(GenerateSegmentedInputFormat.class);

	private static GenerateSegmentedInputFormat formatter = null;

	public static GenerateSegmentedInputFormat getInstance() {

		if (formatter == null) {
			formatter = new GenerateSegmentedInputFormat();
		}
		return formatter;
	}

	public static boolean isInputFormat(String sheetName) {
		return sheetName.startsWith("Format entree segments");
	}

	@Override
	public List<String> getSeparators() {
		return Arrays.asList(new String[] {"\n", "|"});
	}
}
