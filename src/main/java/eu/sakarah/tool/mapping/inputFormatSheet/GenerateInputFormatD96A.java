package eu.sakarah.tool.mapping.inputFormatSheet;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateInputFormatD96A extends AbstractGenerateSegmentedInputFormat {

	protected static final Logger LOGGER = LoggerFactory.getLogger(GenerateInputFormatD96A.class);

	private static GenerateInputFormatD96A formatter = null;

	public static GenerateInputFormatD96A getInstance() {

		if (formatter == null) {
			formatter = new GenerateInputFormatD96A();
		}
		return formatter;
	}

	public static boolean isInputFormat(String sheetName) {
		return sheetName.startsWith("Format entree D96A");
	}

	@Override
	public List<String> getSeparators() {
		return Arrays.asList(new String[] {"'!$", "+", ":"});
	}
}
