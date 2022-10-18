package eu.sakarah.tool.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.sakarah.tool.mapping.inputFormatSheet.AbstractGenerateSegmentedInputFormat;
import eu.sakarah.tool.mapping.inputFormatSheet.GenerateInputFormatD96A;
import eu.sakarah.tool.mapping.inputFormatSheet.GenerateSegmentedInputFormat;
import eu.sakarah.tool.mapping.mappingSheet.GenerateMappingTransformation;
import eu.sakarah.tool.mapping.mappingSheet.GenerateSmooksConfiguration;

public class GenerateMappingsFromExcelFile {

	protected static final Logger LOGGER = LoggerFactory.getLogger(GenerateMappingsFromExcelFile.class);

	public void generate(File excelFile, File outputDirectory) throws Exception {
		LOGGER.info("Traitement du fichier " + excelFile.getCanonicalPath());
		InputStream excelStream = new FileInputStream(excelFile);
		generate(excelStream, outputDirectory);
	}

	public void generate(InputStream excelStream, File outputDirectory) throws Exception {

		XSSFWorkbook workBook = new XSSFWorkbook(excelStream);
		try {
			AbstractGenerateSegmentedInputFormat inputFormatReader = null;
			XSSFSheet inputFormat = null;
			XSSFSheet inputMapping = null;
			List<XSSFSheet> outputMappings = new ArrayList<XSSFSheet>();

			// Filter useful sheets.
			for (int sheetIndex = 0; sheetIndex < workBook.getNumberOfSheets(); sheetIndex++) {
				XSSFSheet sheet = workBook.getSheetAt(sheetIndex);
				if (GenerateMappingTransformation.isInputMapping(sheet)) {
					if (inputMapping != null) {
						throw new RuntimeException("Plusieurs mappings d'entrée ont été détectés. Il ne peut y en avoir qu'un seul.");
					}
					inputMapping = sheet;
				} else if (GenerateInputFormatD96A.isInputFormat(sheet.getSheetName())) {
					// Format d'entrée : D96A.
					if (inputFormat != null) {
						throw new RuntimeException("Plusieurs onglets de description du format d'entrée ont été détectés. Il ne peut y en avoir qu'un seul.");
					}
					inputFormatReader = GenerateInputFormatD96A.getInstance();
					inputFormat = sheet;
				} else if (GenerateSegmentedInputFormat.isInputFormat(sheet.getSheetName())) {
					// Format d'entrée : suite de segments.
					if (inputFormat != null) {
						throw new RuntimeException("Plusieurs onglets de description du format d'entrée ont été détectés. Il ne peut y en avoir qu'un seul.");
					}
					inputFormatReader = GenerateSegmentedInputFormat.getInstance();
					inputFormat = sheet;
				} else if (GenerateMappingTransformation.isOutputMapping(sheet)) {
					outputMappings.add(sheet);
				}
			}

			if (inputMapping != null) {

				File inputMappingDirectory = new File(outputDirectory, GenerateMappingTransformation.generateMappingDirectoryName(inputMapping.getSheetName()));
				File intermediaryTransformation = null;
				if (inputFormat != null) {
					LOGGER.info("Traitement du format d'entrée de l'onglet " + inputFormat.getSheetName());
					intermediaryTransformation = inputFormatReader.generate(inputFormat, inputMappingDirectory);
				}
				LOGGER.info("Traitement du mapping d'entrée de l'onglet " + inputMapping.getSheetName());
				String rootXPath = GenerateMappingTransformation.generate(inputMapping, inputMappingDirectory);
				GenerateSmooksConfiguration.generate(rootXPath, true, intermediaryTransformation, inputMappingDirectory);
			}

			for (XSSFSheet outputMapping : outputMappings) {

				LOGGER.info("Traitement du mapping de sortie de l'onglet " + outputMapping.getSheetName());
				File outputMappingDirectory = new File(outputDirectory, GenerateMappingTransformation.generateMappingDirectoryName(outputMapping.getSheetName()));
				String rootXPath = GenerateMappingTransformation.generate(outputMapping, outputMappingDirectory);
				GenerateSmooksConfiguration.generate(rootXPath, false, null, outputMappingDirectory);
			}

		} finally {
			workBook.close();
		}
	}
}
