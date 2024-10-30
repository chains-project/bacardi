package se.kth.extractor;

import se.kth.Util.FileUtils;
import se.kth.Util.JsonUtils;
import se.kth.listener.CustomExecutionListener.TestResult;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TrueFailingTestCasesProvider {

    private final Path collectedBreakingTestTracesBaseDir;
    private final Map<String, List<String>> preFailingTestCases;

    public TrueFailingTestCasesProvider(Path collectedBreakingTestTracesBaseDir, Path unsuccessfulTestCasesJson) {
        this.collectedBreakingTestTracesBaseDir = collectedBreakingTestTracesBaseDir;
        this.preFailingTestCases = JsonUtils.readFromFile(unsuccessfulTestCasesJson, Map.class);
    }

    public List<TestResult> getTrueFailingTestCases(String commitId) {
        Path outputDir = collectedBreakingTestTracesBaseDir.resolve(commitId);
        List<File> testResultFiles = FileUtils.getFilesInDirectory(outputDir.toString());

        return testResultFiles.stream()
                .map(file -> (TestResult) FileUtils.readFromBinary(file.getPath()))
                .filter(testResult -> testResult.throwable != null)
                .filter(testResult -> isTrueFailingTestCase(commitId, testResult.testIdentifier))
                .toList();
    }

    private boolean isTrueFailingTestCase(String commitId, String testIdentifier) {
        return !(this.preFailingTestCases.containsKey(commitId) && this.preFailingTestCases.get(commitId).contains(testIdentifier));
    }
}
