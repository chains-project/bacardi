package se.kth.failure_detection;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import se.kth.japicmp_analyzer.ApiChange;
import se.kth.models.ErrorInfo;
import spoon.reflect.declaration.CtElement;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@ToString
@EqualsAndHashCode
/*
 * Represents a file with detected errors, including API changes and executed elements.
 */
public class DetectedFileWithErrors {

    /**
     * A set of API changes detected in the file.
     */
    private Set<ApiChange> apiChanges;

    /**
     * A set of executed elements in the file.
     */
    private Set<CtElement> executedElements;

    /**
     * Information about the error detected in the file.
     */
    private ErrorInfo errorInfo;

    private String lineInCode;

    /*
     * Path to the class file.
     */
    private String classPath;

    private CtElement codeElement;

    /**
     * Information about the fault detected in the file.
     */
    public String methodName;
    public String methodCode;
    public String qualifiedMethodCode;
    public String qualifiedInClassCode;
    public String inClassCode;
    public String plausibleDependencyIdentifier;
    public int clientLineNumber;
    public int clientEndLineNumber;


    /**
     * Constructs a new DetectedFileWithErrors instance with the specified API changes and executed elements.
     *
     * @param apiChanges the set of API changes detected in the file
     * @param element    the set of executed elements in the file
     */
    public DetectedFileWithErrors(Set<ApiChange> apiChanges, Set<CtElement> element) {
        this.apiChanges = apiChanges;
        this.executedElements = element;
    }

    public DetectedFileWithErrors(Set<ApiChange> apiChanges, CtElement element) {
        this.apiChanges = apiChanges;
        this.codeElement = element;
    }

    public DetectedFileWithErrors(ErrorInfo errorInfo) {
        this.apiChanges = new HashSet<>();
        this.errorInfo = errorInfo;
        this.executedElements = null;
        this.classPath = null;
    }

    public DetectedFileWithErrors(ErrorInfo errorInfo, String lineInCode) {
        this.apiChanges = new HashSet<>();
        this.errorInfo = errorInfo;
        this.executedElements = null;
        this.classPath = null;
        this.lineInCode = lineInCode;
    }
}
