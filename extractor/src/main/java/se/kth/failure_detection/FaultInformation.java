package se.kth.failure_detection;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import spoon.reflect.declaration.CtElement;


@Setter
@Getter
@lombok.experimental.Accessors(chain = true)
public class FaultInformation {
    public String methodName;
    public String methodCode;
    public String qualifiedMethodCode;
    public String qualifiedInClassCode;
    public String inClassCode;
    public String plausibleDependencyIdentifier;
    public int clientLineNumber;
    public int clientEndLineNumber;

    public FaultInformation() {
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
}
