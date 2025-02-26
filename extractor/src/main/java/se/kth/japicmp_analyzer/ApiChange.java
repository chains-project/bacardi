package se.kth.japicmp_analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import japicmp.model.JApiCompatibilityChange;
import lombok.experimental.Accessors;
import se.kth.spoon.ApiMetadata;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

@lombok.Setter
@lombok.Getter
@Accessors(chain = true)
public class ApiChange {
    private int modifier;
    private String returnType;
    private String element;
    private ApiChangeType action;
    private String longName;

    private List<JApiCompatibilityChange> category;
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
        if (this.action.equals(ApiChangeType.REMOVE)) {
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

    public boolean isSame(ApiChange apiChange) {
        return this.getCompleteValue().equals(apiChange.getCompleteValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiChange that = (ApiChange) o;
        return Objects.equals(modifier, that.modifier)
                && Objects.equals(returnType, that.returnType)
                && Objects.equals(element, that.element)
                && Objects.equals(action, that.action)
                && Objects.equals(longName, that.longName)
                && Objects.equals(category, that.category)
                && Objects.equals(description, that.description)
                && Objects.equals(name, that.name)
                && Objects.equals(newVersion, that.newVersion)
                && Objects.equals(oldVersion, that.oldVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifier, returnType, element, action, longName, category, description, name, newVersion, oldVersion);
    }

}

