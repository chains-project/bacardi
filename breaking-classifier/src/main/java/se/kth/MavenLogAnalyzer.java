package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenLogAnalyzer {

    /**
     * Patterns to identify different types of failures in the Maven log.
     */
    public static final Map<Pattern, FailureCategory> FAILURE_PATTERNS = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(MavenLogAnalyzer.class);

    static {
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(class file has wrong version (\\d+\\.\\d+), should be (\\d+\\.\\d+))"),
                FailureCategory.JAVA_VERSION_FAILURE);
                FAILURE_PATTERNS.put(Pattern.compile("(?i)(\\[ERROR] Tests run:|There are test failures|There were test failures|" +
                        "Failed to execute goal org\\.apache\\.maven\\.plugins:maven-surefire-plugin)"),
                FailureCategory.TEST_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(warnings found and -Werror specified)"),
                FailureCategory.WERROR_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(COMPILATION ERROR|Failed to execute goal io\\.takari\\.maven\\.plugins:takari-lifecycle-plugin.*?:compile)"),
                FailureCategory.COMPILATION_FAILURE);
    }

    private final File logFile;

    public MavenLogAnalyzer(File logFile) {
        this.logFile = logFile;
    }


    /**
     * Analyze the Maven log file and return the failure category.
     *
     * @return the failure category @see FailureCategory
     */
    public FailureCategory analyze() {

        try {
            String logContent = Files.readString(logFile.toPath());
            for (Map.Entry<Pattern, FailureCategory> entry : FAILURE_PATTERNS.entrySet()) {
                Pattern pattern = entry.getKey();
                Matcher matcher = pattern.matcher(logContent);
                if (matcher.find()) {
                    return entry.getValue();
                }
            }
            return FailureCategory.UNKNOWN_FAILURE;

        } catch (IOException e) {
            log.error("Failed to read log file: {}", logFile.getAbsolutePath());
            throw new RuntimeException(e);
        }

    }


}
