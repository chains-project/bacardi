package se.kth.wError;


import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.kth.DockerBuild;
import se.kth.models.FailureCategory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to repair the wError configuration file.
 */

@Getter
@Setter
public class RepairWError {

    /*
     * The tag for the failOnWarning option in the wError configuration file.
     */
    private final String failOnWarningTag = "failOnWarning";

    private final Logger log = LoggerFactory.getLogger(RepairWError.class);

    private final Path clientCode;
    private Boolean isBump = false;
    private String actualImage;


    public RepairWError(Path clientCode, Boolean isBump, String actualImage) {
        this.clientCode = clientCode;
        this.isBump = isBump;
        this.actualImage = actualImage;
    }


    /**
     * Checks if the log file contains a Java version incompatibility error related to wError.
     *
     * @param logFilePath the path to the log file to be checked
     * @return true if the log file contains the Java version incompatibility error pattern, false otherwise
     * @throws IOException if an I/O error occurs reading the log file
     */
    public boolean isWerrorJavaVersionIncompatibilityError(String logFilePath) throws IOException {
        String javaVersionIncompatibilityPattern = "system modules path not set in conjunction with -source 11";
        return findPattern(logFilePath, javaVersionIncompatibilityPattern);
    }

    /**
     * Searches for a specific pattern in the given log file.
     *
     * @param logFilePath the path to the log file to be searched
     * @param pattern     the regex pattern to search for in the log file
     * @return true if the pattern is found in the log file, false otherwise
     * @throws IOException if an I/O error occurs reading the log file
     */
    public boolean findPattern(String logFilePath, String pattern) throws IOException {
        try {
            FileInputStream fileInputStream = new FileInputStream(logFilePath);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.ISO_8859_1);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;

            Pattern errorPattern = Pattern.compile(pattern);

            while ((line = reader.readLine()) != null) {
                Matcher matcher = errorPattern.matcher(line);
                if (matcher.find()) {
                    return true;
                }
            }
            reader.close();
        } catch (IOException e) {
            log.error("An error occurred while reading the log file: {}", e.getMessage());
            throw e;
        }
        return false;
    }

    /**
     * Replaces the Java version in the `pom.xml` file with the specified version.
     * Creates a backup of the original `pom.xml` file before making changes.
     *
     * @param pomFilePath the path to the `pom.xml` file
     * @param javaVersion the Java version to set in the `pom.xml` file
     * @throws IOException if an I/O error occurs during file operations
     */
    public void replaceJavaVersion(String pomFilePath, int javaVersion) throws IOException {
        File pomFile = new File(pomFilePath);
        File backupPomFile = new File(pomFilePath.replace(".xml", "_backup.xml"));

        // Create a backup copy of the original pom.xml file
        Files.copy(pomFile.toPath(), backupPomFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile.getAbsolutePath());

            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            // Update the Java version in the pom.xml file
            NodeList mavenCompilerSource = root.getElementsByTagName("maven.compiler.source");
            NodeList mavenCompilerTarget = root.getElementsByTagName("maven.compiler.target");
            NodeList mavenCompilerFailOnWarning = root.getElementsByTagName("maven.compiler.failOnWarning");
            NodeList properties = root.getElementsByTagName(failOnWarningTag);

            if (mavenCompilerSource.getLength() > 0) {
                mavenCompilerSource.item(0).setTextContent(String.valueOf(javaVersion));
            }
            if (mavenCompilerTarget.getLength() > 0) {
                mavenCompilerTarget.item(0).setTextContent(String.valueOf(javaVersion));
            }
            if (mavenCompilerFailOnWarning.getLength() > 0) {
                mavenCompilerFailOnWarning.item(0).setTextContent("false");
            }
            if (properties.getLength() > 0) {
                properties.item(0).setTextContent("false");
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            // Save the modified content back to the original pom.xml file
            StreamResult result = new StreamResult(pomFile);
            transformer.transform(source, result);

        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public String reproduce() throws IOException {

        // create docker client
        DockerBuild dockerBuild = new DockerBuild(true);

        dockerBuild.createBaseImageForBreakingUpdate(clientCode, "", actualImage);

        // identify docker image and reproduce changes
        dockerBuild.reproduce(actualImage, FailureCategory.WERROR_FAILURE, clientCode);

        return actualImage;
    }


}
