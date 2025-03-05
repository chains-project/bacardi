package se.kth.direct_failures;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.DockerBuild;
import se.kth.MavenErrorInformation;
import se.kth.SpoonConstructExtractor;
import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.japicmp_analyzer.JApiCmpAnalyze;
import se.kth.model.DependencyTree;
import se.kth.model.SetupPipeline;
import se.kth.models.ErrorInfo;
import se.kth.models.FailureCategory;
import se.kth.models.MavenErrorLog;
import se.kth.parse.ParseMavenDependencyTree;
import se.kth.parse.PomModel;
import se.kth.spoon.ApiMetadata;
import se.kth.spoon.Client;
import se.kth.spoon.SpoonUtilities;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static se.kth.Util.Constants.PIPELINE;

public class RepairDirectFailures {

    private static Logger log = LoggerFactory.getLogger(RepairDirectFailures.class);
    private final DockerBuild dockerBuild;
    private final SetupPipeline setupPipeline;

    public RepairDirectFailures(DockerBuild dockerBuild, SetupPipeline setupPipeline) {
        this.dockerBuild = dockerBuild;
        this.setupPipeline = setupPipeline;
    }

    public Map<String, Set<DetectedFileWithErrors>> basePipeLine() {

        MavenErrorInformation mavenErrorInformation = new MavenErrorInformation(
                setupPipeline.getLogFilePath().toFile());
        // Extracting the line numbers with paths
        Map<String, Set<DetectedFileWithErrors>> detectedFiles = new HashMap<>();

        try {
            MavenErrorLog errorLog = mavenErrorInformation
                    .extractLineNumbersWithPaths(setupPipeline.getLogFilePath().toString());

            errorLog.getErrorInfo().forEach((key, value) -> {
                Set<DetectedFileWithErrors> detectedFileWithErrors = new HashSet<>();
                value.forEach(errorInfo -> {
                    detectedFileWithErrors.add(new DetectedFileWithErrors(errorInfo));

                });
                detectedFiles.put(key, detectedFileWithErrors);
            });

            return detectedFiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Map<String, Set<DetectedFileWithErrors>> extractConstructsFromDirectFailures() throws IOException {
        // Maven error information
        MavenErrorInformation mavenErrorInformation = new MavenErrorInformation(
                setupPipeline.getLogFilePath().toFile());
        // Extracting the line numbers with paths
        MavenErrorLog errorLog = mavenErrorInformation
                .extractLineNumbersWithPaths(setupPipeline.getLogFilePath().toString());

        // Setting the path of the new and old jar files
        Path newJarPath = setupPipeline.getClientFolder().getParent()
                .resolve("%s-%s.jar".formatted(setupPipeline.getBreakingUpdate().updatedDependency.dependencyArtifactID,
                        setupPipeline.getBreakingUpdate().updatedDependency.newVersion));
        Path oldJarPath = setupPipeline.getClientFolder().getParent()
                .resolve("%s-%s.jar".formatted(setupPipeline.getBreakingUpdate().updatedDependency.dependencyArtifactID,
                        setupPipeline.getBreakingUpdate().updatedDependency.previousVersion));
        // Setting the new and old jar files with Metadata
        ApiMetadata newApi = new ApiMetadata(newJarPath.getFileName().toString(), newJarPath);
        ApiMetadata oldApi = new ApiMetadata(oldJarPath.getFileName().toString(), oldJarPath);
        // Analyzing the API changes
        JApiCmpAnalyze japicmpAnalyzer = new JApiCmpAnalyze(oldApi, newApi);
        // Setting the client folder
        Client client = new Client(setupPipeline.getClientFolder());
        // Setting the classpath
        client.setClasspath(List.of(oldJarPath));
        // Extracting the results from the client folder
        SpoonUtilities spoonResults = new SpoonUtilities(client);
        // Extracting the constructs that caused the failure
        SpoonConstructExtractor causingConstructExtractor = new SpoonConstructExtractor(errorLog, japicmpAnalyzer,
                spoonResults, PIPELINE.toString());
        // Extracting the files with errors
        return causingConstructExtractor.extractCausingConstructs();
    }

    public Path generateDependencyTree(Path treeFile, String dockerImage, String projectPath) {

        String[] command = {"/bin/sh", "-c",
                "mvn dependency:3.7.0:tree -DoutputType=json -Dverbose=true -DoutputFile=tree.json"};

        String containerId = dockerBuild.startSpinningContainer(dockerImage);

        String outputExecution = dockerBuild.executeInContainer(containerId, command);
        if (outputExecution.contains("BUILD FAILURE")) {
            log.error("Error while generating dependency tree");
            return null;
        }
        return dockerBuild.copyFromContainer(containerId, projectPath.concat("/tree.json"), treeFile);
    }

    public List<DependencyTree> identifyConflicts(Path mavenDependencyTreeFile) {
        List<DependencyTree> dependencyTrees = checkDependencyResolutionConflict(mavenDependencyTreeFile.toString());
        if (dependencyTrees.isEmpty()) {
            log.info("No conflicts found");
        } else {
            log.info("Conflicts found");
            dependencyTrees.forEach(dependencyTree -> log.info(dependencyTree.toString()));
        }

        return dependencyTrees;
    }

    public String reproduce() {

        try {
            dockerBuild.copyFolderToDockerImage(setupPipeline.getDockerImage(),
                    setupPipeline.getClientFolder().toString());

            Path logFilePath = setupPipeline.getLogFilePath().getParent().resolve("output.log");

            dockerBuild.reproduce(setupPipeline.getDockerImage(), FailureCategory.COMPILATION_FAILURE,
                    setupPipeline.getClientFolder(), logFilePath);

            setupPipeline.setLogFilePath(logFilePath);

            return setupPipeline.getDockerImage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void modifyPomFile(Path pomFile, DependencyTree dependencyTree) throws IOException {
        //
        PomModel pomModel = new PomModel(pomFile);
        File pom = new File(pomFile.toString());
        File backupPomFile = new File(pomFile.toString().replace(".xml", "_backup.xml"));
        Files.copy(pom.toPath(), backupPomFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try {
            Model newModel = pomModel.modifyDependency(dependencyTree);

            // create new pom file
            OutputStream outputStream = Files.newOutputStream(pomFile);
            MavenStaxWriter writer = new MavenStaxWriter();
            writer.write(outputStream, newModel);
        } catch (IOException | XMLStreamException e) {
            log.error("Error modifying the pom file", e);
            throw new RuntimeException(e);
        }
    }

    public List<DependencyTree> checkDependencyResolutionConflict(String dependencyTreePath) {

        ParseMavenDependencyTree parseMavenDependencyTree = new ParseMavenDependencyTree(dependencyTreePath);
        List<DependencyTree> dependencies = parseMavenDependencyTree.parseDependencies();
        List<DependencyTree> children = parseMavenDependencyTree.findChildrenByName(dependencies,
                this.setupPipeline.getBreakingUpdate().updatedDependency.dependencyGroupID,
                this.setupPipeline.getBreakingUpdate().updatedDependency.dependencyArtifactID);
        return parseMavenDependencyTree.hasSameOrLowerLevelDependencies(dependencies, children);
    }

    public Map<String, Set<DetectedFileWithErrors>> buggyLinePipeLine() {
        MavenErrorInformation mavenErrorInformation = new MavenErrorInformation(
                setupPipeline.getLogFilePath().toFile());
        // Extracting the line numbers with paths
        Map<String, Set<DetectedFileWithErrors>> detectedFiles = new HashMap<>();

        try {
            MavenErrorLog errorLog = mavenErrorInformation
                    .extractLineNumbersWithPaths(setupPipeline.getLogFilePath().toString());

            errorLog.getErrorInfo().forEach((key, value) -> {
                Set<DetectedFileWithErrors> detectedFileWithErrors = new HashSet<>();
                value.forEach(errorInfo -> {
                    String buggyLine = getBuggyLine(errorInfo, setupPipeline.getClientFolder().getParent().toString());
                    if (buggyLine.isEmpty()) {
                        log.error("Buggy line not found for error: {}", errorInfo.toString());
                    }
                    detectedFileWithErrors.add(new DetectedFileWithErrors(errorInfo, buggyLine));
                });
                detectedFiles.put(key, detectedFileWithErrors);
            });

            return detectedFiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getBuggyLine(ErrorInfo errorInfo, String clientFolder) {
        Path filePath = Path.of(clientFolder, errorInfo.getClientFilePath());
        if (!Files.exists(filePath)) {
            log.error("File does not exist: {}", filePath);
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String currentLine;
            int currentLineNumber = 0;

            // Iterate through lines
            while ((currentLine = reader.readLine()) != null) {
                currentLineNumber++;
                if (currentLineNumber == Integer.parseInt(errorInfo.getClientLinePosition())) {
                    return currentLine.strip();
                }
            }
        } catch (IOException e) {
            log.error("Error reading the file: {}", e.getMessage(), e);
        }
        return "";
    }

    public Map<String, Set<DetectedFileWithErrors>> extractConstructsFromDirectFailuresWithBuggyLine() {

        try {
            Map<String, Set<DetectedFileWithErrors>> constructsList = extractConstructsFromDirectFailures();
            Map<String, Set<DetectedFileWithErrors>> detectedFiles = buggyLinePipeLine();

            constructsList.forEach((key, value) -> {
                if (detectedFiles.containsKey(key)) {
                    //all files with buggy line
                    Set<DetectedFileWithErrors> detectedFileSet = detectedFiles.get(key);
                    //for each error info inside constructs
                    value.forEach(errorInfo -> {
                        detectedFileSet.forEach(d -> {
                            if (d.getErrorInfo().getErrorMessage().equals(errorInfo.getErrorInfo().getErrorMessage())) {
                                errorInfo.setLineInCode(d.getLineInCode());
                            }
                        });
                    });
                }

            });


            return constructsList;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
