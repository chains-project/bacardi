package se.kth.models;

public enum FailureCategory {
    /**
     * There were unknown failures after updating the dependency, but none in the previous commit.
     */
    UNKNOWN_FAILURE,
    /**
     * There were failures when wError options is enabled.
     */
    WERROR_FAILURE,
    /**
     * There were test failures after updating the dependency.
     */
    TEST_FAILURE,
    /**
     * The compilation failed after updating the dependency, the new version of the dependency new different java version.
     */
    JAVA_VERSION_FAILURE,
    /**
     * The compilation failed after updating the dependency, for code used from indirect dependency .
     */
    TRANSITIVE_DEPENDENCY_FAILURE,
    /**
     * The compilation failed after updating the dependency
     */
    COMPILATION_FAILURE,
    ENFORCER_FAILURE,
    DEPENDENCY_RESOLUTION_FAILURE,
    DEPENDENCY_LOCK_FAILURE,

    NOT_REPAIRED,
    /**
     * Response from the model is not as expec  ted
     */
    ERROR_MODEL_RESPONSE,


    MISSING_API_DIFF, /*
     * Only for repair purposes
     */
    WITH_API_DIFF, BUILD_SUCCESS
}
