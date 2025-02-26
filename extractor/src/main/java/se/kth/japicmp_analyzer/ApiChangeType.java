package se.kth.japicmp_analyzer;


public enum ApiChangeType {
    REMOVE,
    ADD;

    @Override
    public String toString() {
        return switch (this) {
            case REMOVE -> "removed";
            case ADD -> "added";
            default -> throw new IllegalArgumentException("Unexpected value: " + this);
        };
    }
}
