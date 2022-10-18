package eu.sakarah.tool.moulinette;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Web Service permettant d'appeler la moulinette.
 */
@RestController
@RequestMapping("/mapping")
public class MappingController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private MappingService mappingService;

    /**
     * Lance la moulinette.
     * Les fichiers produits par la moulinette sont renvoyés dans un zip.
     *
     * @param mappingFile Fichier excel de mapping
     * @param sampleInput Fichier de test
     * @param mappingName Nom du mapping à appliquer au fichier de test
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "run", method = RequestMethod.POST)
    public void run(
            @RequestParam("mapping") MultipartFile mappingFile,
            @RequestParam(value = "sample", required = false) MultipartFile sampleInput,
            @RequestParam(value = "mappingName", required = false) String mappingName,
            @RequestParam(value = "base64", required = false, defaultValue = "false") boolean base64,
            HttpServletResponse response)
            throws Exception
    {
        String suffix = "_" + TIME_FORMATTER.format(LocalDateTime.now()) + "_result.zip";
        response.addHeader("Content-disposition", "attachment;filename=" + mappingFile.getOriginalFilename().replaceFirst("\\.[^.]*$", suffix));
        response.setContentType(MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);

        ServletOutputStream outputStream = response.getOutputStream();

        if (base64) {
            Base64OutputStream base64OutputStream = new Base64OutputStream(outputStream, true, 76, new byte[]{});
            mappingService.generate(getInputStream(mappingFile), getInputStream(sampleInput), base64OutputStream, mappingName);
            base64OutputStream.close();
        } else {
            mappingService.generate(getInputStream(mappingFile), getInputStream(sampleInput), outputStream, mappingName);
        }

        // response.flushBuffer();
    }

    private static InputStream getInputStream(MultipartFile file) {
        try {
            return file != null && !file.isEmpty() ? file.getInputStream() : null;
        }
        catch (IOException e) {
            LOGGER.info("Erreur lors de la lecture de {}", file.getOriginalFilename(), e);
            return null;
        }
    }

}
