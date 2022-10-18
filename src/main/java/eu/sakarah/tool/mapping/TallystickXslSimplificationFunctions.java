package eu.sakarah.tool.mapping;

public class TallystickXslSimplificationFunctions {

	public static String countDistinct(String source) {

		return "count(" + source + "[not(preceding::" + source + "/. = .)]";
	}
}
