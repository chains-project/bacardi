package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.kth.models.ErrorInfo;
import se.kth.models.WerrorInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WerrorInformation extends MavenErrorInformation {

    private final String failOnWarningTag = "failOnWarning";
    private final String wError = "Werror";
    private final String fileName = "pom.xml";

    private final Logger log = LoggerFactory.getLogger(WerrorInformation.class);


    public WerrorInformation(File logFile) {
        super(logFile);
    }


    /**
     * Extract the Werror line from the log file
     *
     * @param logFilePath the path to the log file
     * @return the MavenErrorLog object containing the Werror line
     * @throws IOException if an I/O error occurs
     */
    public MavenErrorLog extractWarningLines(String logFilePath) throws IOException {

        MavenErrorLog mavenErrorLogs = new MavenErrorLog();

        try {
            FileInputStream fileInputStream = new FileInputStream(logFilePath);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.ISO_8859_1);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            String currentPath = null;
            Pattern errorPattern = Pattern.compile("\\[WARNING\\] .*:\\[(\\d+),\\d+\\]");
            Pattern pathPattern = Pattern.compile("/[^:/]+(/[^\\[\\]:]+)");

            int lineNumberInFile = 0;
            while ((line = reader.readLine()) != null) {
                lineNumberInFile++;
                Map<Integer, String> lines = new HashMap<>();
                Matcher matcher = errorPattern.matcher(line);
                if (matcher.find()) {
                    Integer lineNumber = Integer.valueOf(matcher.group(1));
                    Matcher pathMatcher = pathPattern.matcher(line);
                    lines.put(lineNumber, line);
                    if (pathMatcher.find()) {
                        currentPath = pathMatcher.group();
                    }
                    if (currentPath != null) {

                        ErrorInfo errorInfo = new ErrorInfo(String.valueOf(lineNumber), currentPath, line, lineNumberInFile, extractAdditionalInfo(reader));
                        errorInfo.setErrorLogGithubLink(generateLogsLink(projectURL, 4, lineNumberInFile));
                        mavenErrorLogs.addErrorInfo(currentPath, errorInfo);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            log.error("Failed to read log file: {}", logFilePath);

            e.printStackTrace();
        }
        return mavenErrorLogs;
    }

    public MavenErrorLog extractWerrorLine(String logFilePath) throws IOException {

        MavenErrorLog mavenErrorLogs = new MavenErrorLog();

        try {
            FileInputStream fileInputStream = new FileInputStream(logFilePath);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.ISO_8859_1);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            String currentPath = null;
            Pattern errorPattern = Pattern.compile("warnings found and -Werror specified");
            Pattern pathPattern = Pattern.compile("/[^:/]+(/[^\\[\\]:]+)");

            int lineNumberInFile = 0;
            while ((line = reader.readLine()) != null) {
                lineNumberInFile++;
                Map<Integer, String> lines = new HashMap<>();
                Matcher matcher = errorPattern.matcher(line);
                if (matcher.find()) {
                    Matcher pathMatcher = pathPattern.matcher(line);
                    if (pathMatcher.find()) {
                        currentPath = pathMatcher.group();
                    }
                    if (currentPath != null) {
                        ErrorInfo errorInfo = new ErrorInfo("", currentPath, line, lineNumberInFile, extractAdditionalInfo(reader));
                        errorInfo.setErrorLogGithubLink(generateLogsLink(projectURL, 4, lineNumberInFile));
                        mavenErrorLogs.addErrorInfo(currentPath, errorInfo);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mavenErrorLogs;
    }


    /**
     * Parses the specified POM file and retrieves elements by the given tag.
     *
     * @param pom the POM file to parse
     * @param tag the tag name to search for in the POM file
     * @return a NodeList containing the elements with the specified tag
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created
     * @throws IOException                  if an I/O error occurs
     * @throws SAXException                 if a parsing error occurs
     */
    private NodeList parsePOM(File pom, String tag) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pom.getAbsolutePath());

        // get the root element
        return doc.getElementsByTagName(tag);
    }


    /**
     * Recursively finds all POM files in the given directory.
     *
     * @param file the directory to search for POM files
     * @return a list of POM files found in the directory
     */
    private List<File> findPomFiles(File file) {
        List<File> pomFiles = new ArrayList<>();

        List<File> search = List.of(Objects.requireNonNull(file.listFiles()));

        for (File f : search) {
            if (f.isDirectory()) {
                pomFiles.addAll(findPomFiles(f));
            } else {
                if (f.getName().equals(fileName)) {
                    pomFiles.add(f);
                }
            }
        }

        return pomFiles;
    }

    public List<File> findWerror(File file) {
        List<File> pomFiles = findPomFiles(file);
        List<File> werrorFiles = new ArrayList<>();

        for (File pom : pomFiles) {
            try {
                NodeList fileOnWarningsNode = parsePOM(pom, failOnWarningTag);

                if (fileOnWarningsNode.getLength() > 0) {
                    werrorFiles.add(pom);
                }

            } catch (ParserConfigurationException | IOException | SAXException e) {
                System.out.println("Error parsing the file " + pom.getAbsolutePath() + " " + e.getMessage());
            }
        }
        return werrorFiles;
    }

    /**
     * Analyzes the Werror in the Maven log file.
     *
     * @param client the client root folder
     * @throws IOException if an I/O error occurs
     */
    public WerrorInfo analyzeWerror(String client) throws IOException {
        // prepare the log file

        // Extract the Werror line from the log file
        MavenErrorLog wError = extractWerrorLine(logFile.getAbsolutePath());


        // Extract the warning lines from the log file
        MavenErrorLog warningLines = extractWarningLines(logFile.getAbsolutePath());


        // find error in the client root folder.
        List<File> werrorFiles = findWerror(new File(client));

        // Create a WErrorMetadata object with the extracted information
        return new WerrorInfo(wError, warningLines, !werrorFiles.isEmpty(), werrorFiles, clientName(client));

    }


    private String clientName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

}
