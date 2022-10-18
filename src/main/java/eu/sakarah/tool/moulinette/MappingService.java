package eu.sakarah.tool.moulinette;

import ch.qos.logback.classic.ClassicConstants;
import eu.sakarah.tool.mapping.GenerateMappingsFromExcelFile;
import eu.sakarah.tool.mapping.mappingSheet.GenerateMappingTransformation;
import eu.sakarah.tool.mapping.mappingSheet.GenerateSmooksConfiguration;
import eu.sakarah.tool.mapping.mappingSheet.XmlSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.milyn.Smooks;
import org.milyn.container.ExecutionContext;
import org.milyn.payload.JavaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipUtil;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Service permettant de lancer la moulinette.
 */
@Service
public class MappingService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingService.class);

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private static final String LOG_DIR_MDC = "logDir";
    private static final String LOG_PATH_MDC = "logPath";

    private final Set<File> toDelete = new HashSet<>();

    /**
     * Appel de la moulinette.
     *
     * @param mapping Fichier excel de mapping
     * @param sample Fichier de test
     * @param result Résultat sous forme d'un zip
     * @param mappingName Nom du mapping à utiliser
     * @throws Exception
     */
    public void generate(InputStream mapping, InputStream sample, OutputStream result, String mappingName) throws Exception {
        File target = generate(mapping, sample, mappingName);
        // Base64OutputStream base64OutputStream = new Base64OutputStream(result, true, 76, new byte[]{});
        // ZipUtil.pack(target, base64OutputStream);
        // base64OutputStream.close();
        // result.flush();
        ZipUtil.pack(target, result);
        synchronized (toDelete) {
            toDelete.add(target);
        }
    }

    /**
     * Appel de la moulinette.
     *
     * @param mapping Fichier excel de mapping
     * @param sample Fichier de test
     * @param mappingName Nom du mapping à utiliser
     * @return Répertoire temporaire contenant les fichiers générés
     * @throws Exception
     */
    private File generate(InputStream mapping, InputStream sample, String mappingName) throws Exception {
        // Répertoire temporaire pour génération des fichiers
        File target = Files.createTempDirectory("moulinette").toFile();

        // Envoie les logs dans le répertoire temp
        MDC.put(LOG_PATH_MDC, target.getAbsolutePath());
        MDC.put(LOG_DIR_MDC, target.getName());

        try {
            GenerateMappingsFromExcelFile generationTool = new GenerateMappingsFromExcelFile();
            generationTool.generate(mapping, target);

            if (sample != null && StringUtils.isNotBlank(mappingName)) {
                try {
                    boolean isInputMapping = GenerateMappingTransformation.isInputMapping(mappingName);
                    LOGGER.info("Test " + (isInputMapping ? "input" : "output") + " mapping " + mappingName);

                    // Read Smooks configuration.
                    Smooks smooks = new Smooks(
                            target.getAbsolutePath().replaceAll("\\\\", "/")
                                    + "/" + GenerateMappingTransformation.generateMappingDirectoryName(mappingName)
                                    + "/" + GenerateSmooksConfiguration.MAPPING_FILE_NAME_SMOOKS_CONFIG);

                    ExecutionContext executionContext = smooks.createExecutionContext();

                    // Add parameters.
                    HashMap<String, Object> tallystickBean = new HashMap<>();
                    tallystickBean.put("ediApplicationVersion", "3.0.0");
                    tallystickBean.put("ediMessageGenerationDate", "20170309000000");
                    executionContext.getBeanContext().addBean("tallystickBean", tallystickBean);

                    // Get the data and filter.
                    StreamSource source = new StreamSource(sample);
                    JavaResult smooksResult = new JavaResult();

                    LOGGER.info("Transformation Smooks");
                    smooks.filterSource(executionContext, source, smooksResult);

                    // Get what's generated.
                    LOGGER.info("Smooks result map :");
                    smooksResult.getResultMap().forEach((k, v) -> LOGGER.info("result {} : {}", k, StringUtils.abbreviate(v.toString(), 100)));

                    String resultDocument = (String) smooksResult.getBean(isInputMapping ? "document" : "invoice");
                    File resultFile = new File(new File(target, GenerateMappingTransformation.generateMappingDirectoryName(mappingName)), "result.xml");
                    FileUtils.forceMkdir(resultFile.getParentFile());
                    // TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                    transformer.setOutputProperty("{http://xml.apache.org/xslt}content-handler", XmlSerializer.class.getCanonicalName());
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

                    Source xmlInput = new StreamSource(new StringInputStream(resultDocument));
                    FileWriter writer = new FileWriter(resultFile);
                    StreamResult xmlOutput = new StreamResult(writer);
                    LOGGER.info("Transformation du fichier de test");
                    transformer.transform(xmlInput, xmlOutput);
                    writer.close();
                }
                catch (Exception e) {
                    LOGGER.info("Erreur pendant la transformation du fichier de test", e);
                }
            }
        }
        finally {
            // fermeture du fichier de log
            LOGGER.info(ClassicConstants.FINALIZE_SESSION_MARKER, "end");
            MDC.remove(LOG_PATH_MDC);
            MDC.remove(LOG_DIR_MDC);
        }

        return target;
    }

    /**
     * Supprime les répertoires temporaires où la moulinette génère ses fichiers.
     * On ne peut pas le faire directement à la fin du traitement car le fichier de log associé au run
     * de la moulinette peut ne pas encore être fermé.
     */
    @Scheduled(fixedDelay = 10_000)
    public void clean() {
        LOGGER.debug("Clean temp dirs");
        synchronized (toDelete) {
            for (Iterator<File> it = toDelete.iterator(); it.hasNext(); ) {
                try {
                    File dir = it.next();
                    FileUtils.deleteDirectory(dir);
                    LOGGER.info("Removed {}", dir);
                    it.remove();
                } catch (IOException e) {
                    LOGGER.debug("Clean error : {}", e.getMessage());
                    // retry
                }
            }
        }
    }

}
