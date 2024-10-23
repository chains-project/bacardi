package se.kth.scripts;

import se.kth.extractor.TestMethodLocalizer;
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
            List<File> allFailures = FileUtils.getFilesInDirectory(project.toString());
            for (File testFailure : allFailures) {
                TestResult testResult = (TestResult) FileUtils.readFromBinary(testFailure.getAbsolutePath());
                if (testResult.throwable != null && testResult.throwable.className.contains("ssertion")) {
                    TestMethodLocalizer testMethodLocalizer = new TestMethodLocalizer(testResult.throwable.stackTrace);
                    testMethodLocalizer.localizePathOfTestMethodFile("");
                }
            }
        }
    }
}
