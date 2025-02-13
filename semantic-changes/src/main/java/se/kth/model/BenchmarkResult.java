package se.kth.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BenchmarkResult {
    @JsonSerialize
    private final String id;
    @JsonSerialize
    private final boolean semB;
    @JsonSerialize
    private final boolean groundTruth;
    @JsonSerialize
    private final boolean semanticanary;

    public BenchmarkResult(String id, boolean semB, boolean groundTruth, boolean semanticanary) {
        this.id = id;
        this.semB = semB;
        this.groundTruth = groundTruth;
        this.semanticanary = semanticanary;
    }

    public String toString() {
        return String.format("semB: %s\nGround truth: %s\nSemanticanary: %s\n", semB, groundTruth, semanticanary);
    }
}
