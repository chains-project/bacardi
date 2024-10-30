package se.kth.analysis;

import org.junit.platform.engine.TestExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.PipelineComponent;
import se.kth.Util.FileUtils;
import se.kth.Util.JsonUtils;
import se.kth.listener.CustomExecutionListener;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestResultAnalyzer extends PipelineComponent {

    private static final Logger logger = LoggerFactory.getLogger(TestResultAnalyzer.class);

    private final Path inputDirectory;
    private final Map<String, List<String>> collectableNonCollectableProjects;
    private final Map<String, List<String>> unsuccessfulTestCasesResult;

    public TestResultAnalyzer(Path inputDirectory) {
        this.inputDirectory = inputDirectory;
        this.collectableNonCollectableProjects = new HashMap<>();
        this.collectableNonCollectableProjects.put("collectable", new LinkedList<>());
        this.collectableNonCollectableProjects.put("nonCollectable", new LinkedList<>());

        this.unsuccessfulTestCasesResult = new HashMap<>();
    }

    @Override
    public void execute(BreakingUpdate breakingUpdate) {
        String commitId = breakingUpdate.breakingCommit;
        Path outputPath = inputDirectory.resolve(commitId);
        if (collectionWorked(outputPath)) {
            this.saveCollectable(commitId);
            this.analyzeFailedTestCases(outputPath, commitId);
        } else {
            this.saveNonCollectable(commitId);
        }
    }

    private void saveCollectable(String commitId) {
        this.collectableNonCollectableProjects.get("collectable").add(commitId);
    }

    private void saveNonCollectable(String commitId) {
        this.collectableNonCollectableProjects.get("nonCollectable").add(commitId);
    }

    private void analyzeFailedTestCases(Path outputPath, String commitId) {
        List<String> unsuccessfulTestCases = new LinkedList<>();
        for (File file : FileUtils.getFilesInDirectory(outputPath.toString())) {
            CustomExecutionListener.TestResult testResult =
                    (CustomExecutionListener.TestResult) FileUtils.readFromBinary(file.getPath());
            if (testResult.status != TestExecutionResult.Status.SUCCESSFUL) {
                unsuccessfulTestCases.add(testResult.testIdentifier);
            }
        }
        if (!unsuccessfulTestCases.isEmpty()) {
            this.unsuccessfulTestCasesResult.put(commitId, unsuccessfulTestCases);
        }
    }

    private boolean collectionWorked(Path outputPath) {
        try {
            return Files.list(outputPath).findFirst().isPresent();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void finish() {
        Path collectableOutputPath = Config.getResourcesDir().resolve("collectable.json");
        JsonUtils.writeToFile(collectableOutputPath, collectableNonCollectableProjects);

        Path unsuccessfulTestCasesOutputPath = Config.getResourcesDir().resolve("unsuccessfulTestCases.json");
        JsonUtils.writeToFile(unsuccessfulTestCasesOutputPath, unsuccessfulTestCasesResult);
    }
}
