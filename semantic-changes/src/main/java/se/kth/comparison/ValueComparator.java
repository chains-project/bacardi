package se.kth.comparison;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import se.kth.matching.Difference;
import se.kth.model.MethodInvocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ValueComparator {

    public static void main(String[] args) throws Exception {
        String json1 = Files.readString(Path.of("/home/leonard/code/java/bacardi/semantic-changes/src/main/resources" +
                "/jsoup_Element.prepend/jsoup_1.7.1_value.json"));
        String json2 = Files.readString(Path.of("/home/leonard/code/java/bacardi/semantic-changes/src/main/resources" +
                "/jsoup_Element.prepend/jsoup_1.7.3_value.json"));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node1 = objectMapper.readTree(json1);
        JsonNode node2 = objectMapper.readTree(json2);

        List<Difference> differences = compare(node1, node2);

        if (differences.isEmpty()) {
            System.out.println("The two JSON objects are deeply equal.");
        } else {
            System.out.println("Differences found:");
            differences.forEach(System.out::println);
        }
    }

    public static List<List<Difference>> compareAll(List<Pair<MethodInvocation, MethodInvocation>> pairs) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<List<Difference>> differences = new ArrayList<>();
        for (Pair<MethodInvocation, MethodInvocation> pair : pairs) {
            MethodInvocation left = pair.getLeft();
            MethodInvocation right = pair.getRight();
            JsonNode leftReturnValue = mapper.readTree(left.getReturnValue());
            JsonNode rightReturnValue = mapper.readTree(right.getReturnValue());
            differences.add(compare(leftReturnValue, rightReturnValue));
        }
        return differences;
    }

    public static List<Difference> compare(JsonNode node1, JsonNode node2) {
        Map<String, JsonNode> referenceMap1 = buildReferenceMap(node1);
        Map<String, JsonNode> referenceMap2 = buildReferenceMap(node2);

        Set<String> visitedPairs = new HashSet<>();

        return compareJson(node1, node2, "", referenceMap1, referenceMap2, visitedPairs);
    }

    private static Map<String, JsonNode> buildReferenceMap(JsonNode root) {
        Map<String, JsonNode> referenceMap = new HashMap<>();
        buildReferenceMapHelper(root, referenceMap);
        return referenceMap;
    }

    private static void buildReferenceMapHelper(JsonNode node, Map<String, JsonNode> referenceMap) {
        if (node.isObject()) {
            JsonNode meta = node.get("__meta__");
            if (meta != null && meta.has("hash")) {
                String hash = meta.get("hash").asText();
                referenceMap.put(hash, node);
            }

            node.fields().forEachRemaining(entry -> buildReferenceMapHelper(entry.getValue(), referenceMap));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                buildReferenceMapHelper(child, referenceMap);
            }
        }
    }

    public static List<Difference> compareJson(JsonNode node1, JsonNode node2, String path,
                                               Map<String, JsonNode> referenceMap1,
                                               Map<String, JsonNode> referenceMap2,
                                               Set<String> visitedPairs) {
        List<Difference> differences = new ArrayList<>();

        if (node1 == null && node2 == null) {
            return differences;
        } else if (node1 == null || node2 == null) {
            differences.add(new Difference(path, "One of the nodes is null"));
            return differences;
        }

        if (!node1.getNodeType().equals(node2.getNodeType())) {
            differences.add(new Difference(path,
                    "Node types differ (" + node1.getNodeType() + " vs " + node2.getNodeType() + ")"));
            return differences;
        }

        if (node1.isObject()) {
            // Track visited pairs to prevent infinite loops
            String hash1 = extractHash(node1);
            String hash2 = extractHash(node2);

            if (hash1 != null && hash2 != null) {
                String pairKey = hash1 + "|" + hash2;
                if (visitedPairs.contains(pairKey)) {
                    return differences; // Already compared these objects
                }
                visitedPairs.add(pairKey);
            }

            Iterator<String> fieldNames1 = node1.fieldNames();
            while (fieldNames1.hasNext()) {
                String fieldName = fieldNames1.next();

                if (!fieldName.equals("hash")) {
                    JsonNode child1 = node1.get(fieldName);
                    JsonNode child2 = node2.get(fieldName);

                    if (child1 != null && child1.isTextual() && child1.asText().startsWith("<circular reference:")) {
                        String refHash1 = extractHashFromReference(child1.asText());
                        JsonNode resolved1 = referenceMap1.get(refHash1);

                        String refHash2 = child2 != null && child2.isTextual() && child2.asText().startsWith(
                                "<circular reference:")
                                ? extractHashFromReference(child2.asText())
                                : null;
                        JsonNode resolved2 = refHash2 != null ? referenceMap2.get(refHash2) : null;

                        differences.addAll(compareJson(resolved1, resolved2, path + "/" + fieldName, referenceMap1,
                                referenceMap2, visitedPairs));
                    } else {
                        differences.addAll(compareJson(child1, child2, path + "/" + fieldName, referenceMap1,
                                referenceMap2, visitedPairs));
                    }
                }
            }

            // Check for fields in node2 not present in node1
            Iterator<String> fieldNames2 = node2.fieldNames();
            while (fieldNames2.hasNext()) {
                String fieldName = fieldNames2.next();
                if (!fieldName.equals("hash") && !node1.has(fieldName)) {
                    differences.add(new Difference(path, fieldName + ": Field is missing in the first object"));
                }
            }
        } else if (node1.isArray()) {
            if (node1.size() != node2.size()) {
                differences.add(new Difference(path, "Array sizes differ (" + node1.size() + " vs " + node2.size() +
                        ")"));
            } else {
                for (int i = 0; i < node1.size(); i++) {
                    differences.addAll(compareJson(node1.get(i), node2.get(i), path + "[" + i + "]", referenceMap1,
                            referenceMap2, visitedPairs));
                }
            }
        } else if (!node1.asText().equals(node2.asText())) {
            differences.add(new Difference(path, "Values differ (" + node1.asText() + " vs " + node2.asText() + ")"));
        }

        return differences;
    }

    private static String extractHash(JsonNode node) {
        JsonNode meta = node.get("__meta__");
        if (meta != null && meta.has("hash")) {
            return meta.get("hash").asText();
        }
        return null;
    }

    private static String extractHashFromReference(String reference) {
        return reference.replace("<circular reference: ", "").replace(">", "").trim();
    }
}

