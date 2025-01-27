package se.kth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class MethodInvocation {

    private final String className;
    private final String methodName;
    private final StackTraceElement[] stackTrace;
    private final String arguments;
    private final String returnValue;

    public MethodInvocation(
            @JsonProperty("className") String className, @JsonProperty("methodName") String methodName,
            @JsonProperty("stackTrace") StackTraceElement[] stackTrace, @JsonProperty("arguments") String arguments,
            @JsonProperty("returnValue") String returnValue) {
        this.className = className;
        this.methodName = methodName;
        this.stackTrace = stackTrace;
        this.arguments = arguments;
        this.returnValue = returnValue;
    }
}
