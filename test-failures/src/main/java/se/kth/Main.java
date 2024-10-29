package se.kth;

import se.kth.injector.TestListenerInjector;
import se.kth.utils.Config;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        Path testListenerTestOutput = Config.relativeToTmpDir("test-output");
        Path testListenerContainerOutput = Config.relativeToTmpDir("test-container");
        Path pomsDirectory = Config.relativeToTmpDir("poms");

        TestListenerInjector testListenerInjector = new TestListenerInjector(testListenerTestOutput,
                testListenerContainerOutput, pomsDirectory);

        Pipeline pipeline = new Pipeline()
                .with(testListenerInjector);

        pipeline.run();
    }
}