package se.kth.listener;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CustomExecutionListener implements TestExecutionListener {

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        System.out.println(testIdentifier.toString() + ": " + testExecutionResult.toString());
        System.out.println("-------------------------------------------------");
        TestResult testResult = new TestResult(testIdentifier.getUniqueId(),
                testExecutionResult.getThrowable().orElse(null));
        saveTestResult(testResult);
    }

    private void saveTestResult(TestResult testResult) {
        String filePath = "/output/test-" + testResult.hashCode() + ".log";
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filePath))) {
            objectOutputStream.writeObject(testResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private record TestResult(String testIdentifier, Throwable throwable) implements Serializable {
    }

}
