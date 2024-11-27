package se.kth;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import se.kth.models.FailureCategory;

/**
 * @param index Getters
 */
public record Attempt(@Getter int index, @Getter FailureCategory failureCategory, boolean successful) {
    @JsonCreator
    public Attempt(
            @JsonProperty("index") int index,
            @JsonProperty("failureCategory") FailureCategory failureCategory,
            @JsonProperty("successful") boolean successful) {
        this.index = index;
        this.failureCategory = failureCategory;
        this.successful = successful;
    }
}

