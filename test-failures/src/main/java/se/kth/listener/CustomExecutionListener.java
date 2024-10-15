package se.kth.listener;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CustomExecutionListener implements TestExecutionListener {

    private static final Path OUTPUT_DIR = Paths.get("/bacardi-output");

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        System.out.println(testIdentifier.toString() + ": " + testExecutionResult.toString());
        System.out.println("-------------------------------------------------");
        TestResult testResult = new TestResult(testIdentifier.getUniqueId(),
                testExecutionResult.getStatus(),
                testExecutionResult.getThrowable().orElse(null));
        saveTestResult(testResult);
    }

    private void saveTestResult(TestResult testResult) {
        Path filePath = OUTPUT_DIR.resolve("test-" + testResult.hashCode() + ".log");
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            objectOutputStream.writeObject(testResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class TestResult implements Serializable {
        public final String testIdentifier;
        public final TestExecutionResult.Status status;
        public final Throwable throwable;

        public TestResult(String testIdentifier, TestExecutionResult.Status status, Throwable throwable) {
            this.testIdentifier = testIdentifier;
            this.status = status;
            this.throwable = throwable;
        }
    }
}
