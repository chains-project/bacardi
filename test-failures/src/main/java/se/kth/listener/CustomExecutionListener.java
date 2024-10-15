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
        ObjectOutputStream objectOutputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filePath.toFile());
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(testResult);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TestResult implements Serializable {

        private static final long serialVersionUID = -5652087931635613645L;

        public final String testIdentifier;
        public final TestExecutionResult.Status status;
        public final SerializableThrowable throwable;

        public TestResult(String testIdentifier, TestExecutionResult.Status status, Throwable throwable) {
            this.testIdentifier = testIdentifier;
            this.status = status;
            this.throwable = throwable == null ? null : new SerializableThrowable(throwable);
        }
    }

    public static class SerializableThrowable implements Serializable {

        private static final long serialVersionUID = 2988580623727952827L;

        public final String className;
        public final String message;
        public final StackTraceElement[] stackTrace;

        public SerializableThrowable(Throwable throwable) {
            this.className = throwable.getClass().getName();
            this.message = throwable.getMessage();
            this.stackTrace = throwable.getStackTrace();
        }
    }
}
