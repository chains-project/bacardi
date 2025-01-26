package se.kth.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class BenchmarkCase {

    private final int id;
    private final String preVersionImageName;
    private final String postVersionImageName;
    private final String targetMethod;
    private final boolean semB;
    private final boolean groundTruth;

    public BenchmarkCase(@JsonProperty("id") int id, @JsonProperty("preVersionImageName") String preVersionImageName,
                         @JsonProperty("postVersionImageName") String postVersionImageName, @JsonProperty(
            "targetMethod") String targetMethod, @JsonProperty("semB") boolean semB,
                         @JsonProperty("groundTruth") boolean groundTruth) {
        this.id = id;
        this.preVersionImageName = preVersionImageName;
        this.postVersionImageName = postVersionImageName;
        this.targetMethod = targetMethod;
        this.semB = semB;
        this.groundTruth = groundTruth;
    }
}
