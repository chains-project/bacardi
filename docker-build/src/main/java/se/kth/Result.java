package se.kth;

import se.kth.models.FailureCategory;

import java.util.ArrayList;
import java.util.List;

@lombok.Getter
@lombok.Setter
public class Result {

    String breakingCommit;
    private FailureCategory originalFailureCategory;
    private List<Attempt> attempts;

    public Result(FailureCategory originalFailureCategory) {
        this.originalFailureCategory = originalFailureCategory;
        this.attempts = new ArrayList<>();
    }

    public Result() {
        this.attempts = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Result{\n" +
                ",\n originalFailureCategory='" + originalFailureCategory + '\'' +
                ",\n attempts=" + attempts +
                '}';
    }
}