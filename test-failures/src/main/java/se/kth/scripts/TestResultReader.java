package se.kth.scripts;

import org.junit.platform.engine.TestExecutionResult;
import se.kth.listener.CustomExecutionListener;
import se.kth.utils.FileUtils;

import java.io.File;
import java.nio.file.Path;

public class TestResultReader {
    public static void main(String[] args) {
        String directory = Path.of(".tmp", "output").toAbsolutePath().toString();

        for (File file : FileUtils.getFilesInDirectory(directory)) {
            CustomExecutionListener.TestResult testResult =
                    (CustomExecutionListener.TestResult) FileUtils.readFromBinary(file.getPath());

            if (testResult.status != TestExecutionResult.Status.SUCCESSFUL) {
                System.out.println(testResult);
            }

            System.out.println(testResult);
        }
    }
}
