package se.kth;


import se.kth.models.FailureCategory;

public record Attempt(
        int index,
        FailureCategory failureCategory,
        boolean isSuccessful
) {


    @Override
    public String toString() {
        return "Attempt{" +
                "index=" + index +
                ", failureCategory=" + failureCategory +
                ", isSuccessful=" + isSuccessful +
                '}';
    }
}
