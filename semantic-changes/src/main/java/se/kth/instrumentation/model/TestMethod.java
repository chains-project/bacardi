package se.kth.instrumentation.model;

import lombok.Getter;

@Getter
public class TestMethod {

    private final TargetMethod targetMethod;

    public TestMethod(TargetMethod targetMethod) {
        this.targetMethod = targetMethod;
    }
}
