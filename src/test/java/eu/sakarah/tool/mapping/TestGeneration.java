package eu.sakarah.tool.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import eu.sakarah.tool.mapping.mappingSheet.XmlSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.payload.JavaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import eu.sakarah.tool.mapping.mappingSheet.GenerateMappingTransformation;
import eu.sakarah.tool.mapping.mappingSheet.GenerateSmooksConfiguration;

public class TestGeneration {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestGeneration.class);

	private List<SpecificationData> testsData = new ArrayList<SpecificationData>();
	{
		/*
		// Test.
		testsData.add(new SpecificationData("src/test/resources/test/test.xlsx", "test",
				new TestData("Correspondance Test Pivot", "src/test/resources/test/testCustomer.xml", "result_test_pivot.xml"),
				new TestData("Correspondance Test Sortie", "src/test/resources/test/testPivot.xml", "result_test_output.xml")
				));
		// Tests format D96A en entrée.
		testsData.add(new SpecificationData("src/test/resources/testsD96A/testD96A.xlsx", "testsD96A",
				new TestData("Correspondance D96A Pivot", "src/test/resources/testsD96A/input.txt", "result_d96a_to_pivot_mapping.xml")
				));
		testsData.add(new SpecificationData("src/test/resources/testsD96A/SegmentsEtGroupesD96A.xlsx", "testsD96A",
				new TestData("Correspondance Pivot", "src/test/resources/testsD96A/SegmentsEtGroupesD96A.txt", "SegmentsEtGroupesD96A_result.xml")
				));
		// Tests format d'entrée sous forme de suite de segments, les champs séparés par des |.
		testsData.add(new SpecificationData("src/test/resources/testsSegments/segmentedInputTest.xlsx", "testsSegments",
				new TestData("Correspondance Pivot", "src/test/resources/testsSegments/segmentedInput.txt", "segmentedInput_result.xml")
				));
		// Spec du mapping SUEZ -> RATP.
		testsData.add(new SpecificationData("src/test/resources/mappingSuezRatp/Correspondance_SUEZ_RATP_Tessi_V9.3.xlsx", "mappingSuezRatp",
				new TestData("Correspondance SUEZ-PIVOT min.", "src/test/resources/mappingSuezRatp/ExportGESTAMI_Factures_20170427-091134653.xml", "result_suez_pivot.xml"),
				new TestData("Correspondance SUEZ-RATP", "src/test/resources/mappingSuezRatp/ExportGESTAMI_Factures_20170427-091134653_pivot.xml", "result_suez_ratp.xml")
				));
		 */
		// Spec du mapping LaRedoute (D96A) -> ?
		// testsData.add(new SpecificationData("src/test/resources/mappingLaRedouteD96A/Mapping Pivot_D96A bestseller v2.4.xlsx", "mappingLaRedouteD96A"));
		// testsData.add(new SpecificationData("src/test/resources/Correspondance_TESSISA_RANDSTAD_V8.xlsx", "randstad"));

		/*
		testsData.add(new SpecificationData("src/test/resources/mappingSuezRatp/Correspondance_SUEZ_RATP_Tessi_V13.xlsx", "mappingSuezRatp",
				new TestData("Correspondance SUEZ-PIVOT min.", "src/test/resources/mappingSuezRatp/ExportGESTAMI_Factures_20170427-091134653.xml", "result_suez_pivot.xml"),
				new TestData("Correspondance SUEZ-RATP", "src/test/resources/mappingSuezRatp/ExportGESTAMI_Factures_20170427-091134653_pivot.xml", "result_suez_ratp.xml")
		));
		//*/

		//*
		testsData.add(new SpecificationData("src/test/resources/mappingSuezEandis/Correspondance_SUEZ_EANDIS_Tessi_V1.xlsx", "mappingSuezEandis",
				new TestData("Correspondance SUEZ-EANDIS", "src/test/resources/mappingSuezEandis/ExportGESTAMI_Factures_20170804-083134819.xml", "result_suez_eandis.xml")
		));
		//*/

		/*
		testsData.add(new SpecificationData("src/test/resources/mappingSuezOrange/Correspondance_SUEZ_Orange_Tessi_V2.1.xlsx", "mappingSuezOrange",
				new TestData("Correspondance Suez-Orange", "src/test/resources/mappingSuezOrange/facture_34300451100550_38012986600014_G040140966_pivot.xml", "result_suez_orange.xml")
		));
		//*/

		/*
		testsData.add(new SpecificationData("src/test/resources/mappingGefcoSapRoma/Correspondance_GEFCO-SAP-Roma_Tessi_V1.0.xlsx", "mappingGefcoSapRoma",
				new TestData("Correspondance Roma-Flux stand", "src/test/resources/mappingGefcoSapRoma/facture_sap-roma_6000129751_std.xml", "result_gefco_sap_roma.xml")
		));
		//*/

		/*
		testsData.add(new SpecificationData("src/test/resources/mappingGefcoSapPrefact/Correspondance_GEFCO-SAP-Prefac_Tessi_V1.0.xlsx", "mappingGefcoSapPrefact",
				new TestData("Correspondance Prefa-Flux stand", "src/test/resources/mappingGefcoSapPrefact/facture_sap-prefact_5106245278_std.xml", "result_gefco_sap_prefact.xml")
		));
		//*/

	}

	@Test
	public void testGeneration() throws Exception {
		// System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
		// System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
		// System.setErr(new java.io.PrintStream("errorlog.txt"));
		testGeneration(testsData);
	}

	private void testGeneration(List<SpecificationData> testsData) throws Exception {

		for (SpecificationData specificationData : testsData) {

			File excelFile = new File(specificationData.getExcelFile());
			File outputDirectory = new File("target", specificationData.getTargetWorkingDirectory());
			FileUtils.forceMkdir(outputDirectory);
			GenerateMappingsFromExcelFile generationTool = new GenerateMappingsFromExcelFile();
			generationTool.generate(excelFile, outputDirectory);

			for (TestData testData : specificationData.getTests()) {

				boolean isInputMapping = GenerateMappingTransformation.isInputMapping(testData.getMappingName());
				LOGGER.info("Test " + (isInputMapping ? "input" : "output") + " mapping " + testData.getMappingName());

				// Read Smooks configuration.
				Smooks smooks = new Smooks("target/" + specificationData.getTargetWorkingDirectory() + "/" + GenerateMappingTransformation.generateMappingDirectoryName(testData.getMappingName()) + "/" + GenerateSmooksConfiguration.MAPPING_FILE_NAME_SMOOKS_CONFIG);
				ExecutionContext executionContext = smooks.createExecutionContext();

				// Add parameters.
				HashMap<String, Object> tallystickBean = new HashMap<String, Object>();
				tallystickBean.put("ediApplicationVersion", "3.0.0");
				tallystickBean.put("ediMessageGenerationDate", "20170309000000");
				executionContext.getBeanContext().addBean("tallystickBean", tallystickBean);

				// Get the data and filter.
				File testXmlFile = new File(testData.getInputFilePath());
				InputStream inputStream = new FileInputStream(testXmlFile);
				StreamSource source = new StreamSource(inputStream);
				JavaResult smooksResult = new JavaResult();
				smooks.filterSource(executionContext, source, smooksResult);

				// Get what's generated.
				String resultDocument = (String) smooksResult.getBean(isInputMapping ? "document" : "invoice");
				File resultFile = new File(new File(outputDirectory, GenerateMappingTransformation.generateMappingDirectoryName(testData.getMappingName())), testData.getOutputFilePath());
				FileUtils.forceMkdir(resultFile.getParentFile());
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				// transformer.setOutputProperty("{http://xml.apache.org/xslt}content-handler", XmlSerializer.class.getCanonicalName());
				// transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				Source xmlInput = new StreamSource(new StringInputStream(resultDocument));
				FileWriter writer = new FileWriter(resultFile);
				StreamResult xmlOutput = new StreamResult(writer);
				transformer.transform(xmlInput, xmlOutput);
			}
		}
	}
}
