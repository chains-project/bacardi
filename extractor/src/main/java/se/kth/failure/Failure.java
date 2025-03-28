package se.kth.failure;


import se.kth.failure_detection.FaultInformation;
import se.kth.japicmp_analyzer.ApiChange;
import spoon.reflect.declaration.CtElement;

import java.util.List;
import java.util.Set;

public class Failure {
    private final Set<ApiChange> apiChanges;
    public FaultInformation detectedFault;
    public CtElement ctElement;


    public Failure(Set<ApiChange> apiChanges, FaultInformation detectedFault) {
        this.apiChanges = apiChanges;
        this.detectedFault = detectedFault;
    }

    public List<ApiChange> getApiChanges() {
        return this.apiChanges.stream()
                .sorted((a, b) -> {
                    return a.getValue().compareTo(b.getValue());
                })
                .toList();
    }
}

