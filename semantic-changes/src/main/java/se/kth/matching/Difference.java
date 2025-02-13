package se.kth.matching;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

@Getter
public class Difference {
    @JsonSerialize
    private final String path;
    @JsonSerialize
    private final String message;
    @JsonSerialize
    private final DifferenceType type;

    public Difference(String path, String message, DifferenceType type) {
        this.path = path;
        this.message = message;
        this.type = type;
    }

    public String toString() {
        return String.format("%s: %s", path, message);
    }
}
