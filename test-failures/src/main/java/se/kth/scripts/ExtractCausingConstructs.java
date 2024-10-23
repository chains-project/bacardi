package se.kth.scripts;

import se.kth.extractor.TestMethodLoader;
import se.kth.listener.CustomExecutionListener.TestResult;
import se.kth.utils.FileUtils;

import java.io.File;
import java.util.List;

public class ExtractCausingConstructs {
    public static void main(String[] args) {
        String breakingContainerDir = "/home/leohus/bacardi/.tmp/breaking-container";
        String breakingOutputDir = "/home/leohus/bacardi/.tmp/breaking-output";

        List<File> breakingOutputs = FileUtils.getDirectoriesInDirectory(breakingOutputDir);

        for (File project : breakingOutputs) {
            String breakingUpdateId = project.getName();
            List<File> allFailures = FileUtils.getFilesInDirectory(project.toString());
            for (File testFailure : allFailures) {
                TestResult testResult = (TestResult) FileUtils.readFromBinary(testFailure.getAbsolutePath());
                if (testResult.throwable != null) {
                    TestMethodLoader testMethodLoader = new TestMethodLoader(testResult.throwable.stackTrace);
                    testMethodLoader.localizeProjectTestFiles(breakingUpdateId);
                }
            }
        }
    }
}
