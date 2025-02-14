package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.models.FailureCategory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract the failure category from the Maven log file.
 */
public class FailureCategoryExtract {

    /**
     * Patterns to identify different types of failures in the Maven log.
     */
    public static final Map<Pattern, FailureCategory> FAILURE_PATTERNS = new LinkedHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(FailureCategoryExtract.class);

    static {
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(class file has wrong version (\\d+\\.\\d+), should be (\\d+\\.\\d+))"),
                FailureCategory.JAVA_VERSION_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(\\[ERROR] Tests run:|There are test failures|There were test failures|" +
                        "Failed to execute goal org\\.apache\\.maven\\.plugins:maven-surefire-plugin)"),
                FailureCategory.TEST_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(warnings found and -Werror specified)"),
                FailureCategory.WERROR_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(COMPILATION ERROR|Failed to execute goal io\\.takari\\.maven\\.plugins:takari-lifecycle-plugin.*?:compile)|Exit code: COMPILATION_ERROR"),
                FailureCategory.COMPILATION_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(BUILD SUCCESS)"),
                FailureCategory.BUILD_SUCCESS);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(Failed to execute goal org\\.apache\\.maven\\.plugins:maven-enforcer-plugin|" +
                        "Failed to execute goal org\\.jenkins-ci\\.tools:maven-hpi-plugin)"),
                FailureCategory.ENFORCER_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(Could not resolve dependencies|\\[ERROR] Some problems were encountered while processing the POMs|" +
                        "\\[ERROR] .*?The following artifacts could not be resolved)"),
                FailureCategory.DEPENDENCY_RESOLUTION_FAILURE);
        FAILURE_PATTERNS.put(Pattern.compile("(?i)(Failed to execute goal se\\.vandmo:dependency-lock-maven-plugin:.*?:check)"),
                FailureCategory.DEPENDENCY_LOCK_FAILURE);

    }

    private final File logFile;

    public FailureCategoryExtract(File logFile) {
        this.logFile = logFile;
    }


    /**
     * Analyze the Maven log file and return the failure category.
     *
     * @return the failure category @see FailureCategory
     */
    public FailureCategory getFailureCategory(File logFilePath) {

        try {
            String logContent = Files.readString(logFilePath.toPath());
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
