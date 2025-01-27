package se.kth.matching;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Difference {
    @JsonSerialize
    private final String path;
    @JsonSerialize
    private final String message;

    public Difference(String path, String message) {
        this.path = path;
        this.message = message;
    }

    public String toString() {
        return String.format("%s: %s", path, message);
    }
}
