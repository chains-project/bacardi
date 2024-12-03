package se.kth.instrumentation.model;

import lombok.Getter;

import java.util.List;

@Getter
public class TargetMethod {

    private final String parentName;
    private final String name;
    private final List<String> parameters;

    public TargetMethod(String parentName, String name, List<String> parameters) {
        this.parentName = parentName;
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return parentName + ":" + name + "(" + String.join(", ", parameters) + ")";
    }
}
