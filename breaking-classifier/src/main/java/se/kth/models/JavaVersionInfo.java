package se.kth.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents information about Java version incompatibilities.
 */
@lombok.Getter
@lombok.Setter
public class JavaVersionInfo {

    // Both Java version incompatibility
    private JavaVersionIncompatibility incompatibility;
    // List of error messages for each Java version incompatibility
    private Map<JavaVersionIncompatibility, Set<String>> diffVersionErrors;
    // List of Java versions in the workflow files
    private Map<String, List<Integer>> javaInWorkflowFiles;
    // List of error messages
    private Set<String> errorMessages;

    /**
     * Default constructor.
     */
    public JavaVersionInfo() {
    }

    /**
     * Constructs a JavaVersionInfo with the specified incompatibility and workflow files.
     *
     * @param incompatibility     the Java version incompatibility
     * @param javaInWorkflowFiles the Java files in the workflow
     */
    public JavaVersionInfo(JavaVersionIncompatibility incompatibility, Map<String, List<Integer>> javaInWorkflowFiles) {
        this.incompatibility = incompatibility;
        this.javaInWorkflowFiles = javaInWorkflowFiles;
    }

    /**
     * Sets the incompatibility version based on the differences in version errors.
     */
    public void setIncompatibilityVersion() {
        diffVersionErrors.forEach((incompatibility, errors) -> {
            if (incompatibility == null) {
                return;
            }
            this.incompatibility = incompatibility;
            return;
        });
    }

    @Override
    public String toString() {
        return "JavaVersionInfo{" +
                "incompatibility=" + incompatibility +
                ",\n diffVersionErrors=" + diffVersionErrors +
                ",\n javaInWorkflowFiles=" + javaInWorkflowFiles +
                ",\n errorMessages=" + errorMessages +
                '}';
    }
}
