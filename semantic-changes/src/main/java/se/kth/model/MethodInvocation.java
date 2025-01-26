package se.kth.model;

import lombok.Getter;

@Getter
public class MethodInvocation {

    private final String className;
    private final String methodName;
    private final StackTraceElement[] stackTrace;
    private final String arguments;
    private final String returnValue;

    public MethodInvocation(String className, String methodName, StackTraceElement[] stackTrace, String arguments,
                            String returnValue) {
        this.className = className;
        this.methodName = methodName;
        this.stackTrace = stackTrace;
        this.arguments = arguments;
        this.returnValue = returnValue;
    }
}
