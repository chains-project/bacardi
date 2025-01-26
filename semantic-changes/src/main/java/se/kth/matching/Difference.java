package se.kth.matching;

public class Difference {
    private final String path;
    private final String message;

    public Difference(String path, String message) {
        this.path = path;
        this.message = message;
    }

    public String toString() {
        return String.format("%s: %s", path, message);
    }
}
