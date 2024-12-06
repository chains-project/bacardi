package se.kth.japicmp_analyzer;

import java.lang.reflect.Modifier;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.Accessors;
import se.kth.spoon.ApiMetadata;

@lombok.Setter
@lombok.Getter
@Accessors(chain = true)
public class ApiChange {
    private int modifier;
    private String returnType;
    private String element;
    private ApiChangeType action;
    private String longName;

    private String category;
    private String description;
    private String name;
    private ApiMetadata newVersion;
    private ApiMetadata oldVersion;

    public ApiChange() {
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String toDiffString() {
        if(this.action.equals(ApiChangeType.REMOVE)) {
            return "-- " + this.getCompleteValue();
        } else {
            return "++ " + this.getCompleteValue();
        }
    }

    public String getValue() {
        return this.element;
    }

    public String getCompleteValue() {
        return Modifier.toString(this.modifier) + " " + this.returnType + " " + this.element;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        ApiChange thatLibraryChange = (ApiChange) that;
        return this.element.equals(thatLibraryChange.element)
                && this.action.equals(thatLibraryChange.action)
                && this.category.equals(thatLibraryChange.category)
                && this.name.equals(thatLibraryChange.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, action, category, name);
    }
}

