package se.kth.matching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import se.kth.model.MethodInvocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Matcher {
    private static final String METHOD_INVOCATION_FILE = "project/method_returns.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<Pair<MethodInvocation, MethodInvocation>> readAndMatch(Path preVersion, Path postVersion) {

        List<MethodInvocation> preVersionInvocations =
                readMethodInvocations(preVersion.resolve(METHOD_INVOCATION_FILE));
        List<MethodInvocation> postVersionInvocations =
                readMethodInvocations(postVersion.resolve(METHOD_INVOCATION_FILE));
        return match(preVersionInvocations, postVersionInvocations);
    }

    private List<MethodInvocation> readMethodInvocations(Path path) {
        List<String> methodInvocationsRaw = readMethodReturnsFile(path);
        return methodInvocationsRaw.stream()
                .map(s -> {
                    try {
                        JsonNode node = objectMapper.readTree(s);
                        return toMethodInvocation(node);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    private MethodInvocation toMethodInvocation(JsonNode node) {
        String className = node.get("className").asText();
        String methodName = node.get("methodName").asText();
        StackTraceElement[] stackTrace = objectMapper.convertValue(node.get("stackTrace"), StackTraceElement[].class);
        String arguments = node.get("arguments").asText();
        String returnValue = node.get("returnValue").asText();
        return new MethodInvocation(className, methodName, stackTrace, arguments, returnValue);
    }

    private List<String> readMethodReturnsFile(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Pair<MethodInvocation, MethodInvocation>> match(List<MethodInvocation> preVersion,
                                                                 List<MethodInvocation> postVersion) {
        return null;
    }
}
