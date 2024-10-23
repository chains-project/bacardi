package se.kth.extractor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class TestMethodLocalizer {

    private final StackTraceElement[] stackTrace;

    public TestMethodLocalizer(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String localizePathOfTestMethodFile(String projectId) {
        /*
        - From the stacktrace -> localize the first class containing "Test"
        - localize the path of the file in the correct "breaking-container" directory
        - read the file and return the content
         */
        Stream<StackTraceElement> testElements = Arrays.stream(stackTrace)
                .filter(e -> e.getFileName().contains("Test"));
        long count = testElements.count();
        if (count != 1) {
            System.out.println("problem");
        } else {
            System.out.println("no");
        }

        /*
        - find the topmost element containing java/base
        - the first one above that that contains "Test" is the test method call
        - trace from there
         */

        return null;
    }


}
