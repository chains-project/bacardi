package se.kth.matching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.model.MethodInvocation;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Matcher {

    private static final Logger logger = LoggerFactory.getLogger(Matcher.class);

    private static final String METHOD_INVOCATION_FILE = "project/method_returns.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<Pair<MethodInvocation, MethodInvocation>> readAndMatch(Path preVersion, Path postVersion) {
        List<MethodInvocation> preVersionInvocations =
                readMethodInvocations(preVersion.resolve(METHOD_INVOCATION_FILE));
        List<MethodInvocation> postVersionInvocations =
                readMethodInvocations(postVersion.resolve(METHOD_INVOCATION_FILE));

        TestMethodLocalizer preTestLocalizer = new TestMethodLocalizer(preVersion.resolve("project"));
        TestMethodLocalizer postTestLocalizer = new TestMethodLocalizer(postVersion.resolve("project"));
        return match(preVersionInvocations, preTestLocalizer, postVersionInvocations, postTestLocalizer);
    }

    private List<MethodInvocation> readMethodInvocations(Path path) {
        List<String> methodInvocationsRaw = readMethodReturnsFile(path);
        return methodInvocationsRaw.stream()
                .map(s -> {
                    try {
                        return objectMapper.readValue(s, MethodInvocation.class);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> readMethodReturnsFile(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Pair<MethodInvocation, MethodInvocation>> match(List<MethodInvocation> preVersion,
                                                                 TestMethodLocalizer preTestLocalizer,
                                                                 List<MethodInvocation> postVersion,
                                                                 TestMethodLocalizer postTestLocalizer) {
        List<Pair<CtMethodImpl, MethodInvocation>> preWithTestMethod = preVersion.stream()
                .map(pre -> Pair.of(preTestLocalizer.locateTestMethods(pre.getStackTrace()), pre))
                .filter(pair -> pair.getLeft().isPresent())
                .map(pair -> Pair.of(pair.getLeft().get(), pair.getRight()))
                .toList();
        List<Pair<CtMethodImpl, MethodInvocation>> postWithTestMethod = postVersion.stream()
                .map(post -> Pair.of(postTestLocalizer.locateTestMethods(post.getStackTrace()), post))
                .filter(pair -> pair.getLeft().isPresent())
                .map(pair -> Pair.of(pair.getLeft().get(), pair.getRight()))
                .toList();

        logger.info("Finished locating test methods for pre and post versions");

        List<Pair<List<MethodInvocation>, List<MethodInvocation>>> matchedByTestMethodName =
                matchByTestMethodName(preWithTestMethod, postWithTestMethod);

        return matchedByTestMethodName.stream()
                .map(pair -> matchOnArgumentsAndOrder(pair.getLeft(), pair.getRight()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }


    public List<Pair<List<MethodInvocation>, List<MethodInvocation>>> matchByTestMethodName(
            List<Pair<CtMethodImpl, MethodInvocation>> preVersion,
            List<Pair<CtMethodImpl, MethodInvocation>> postVersion) {

        List<Pair<List<MethodInvocation>, List<MethodInvocation>>> matchedInvocations = new ArrayList<>();

        var preGroupedByName = preVersion.stream()
                .collect(Collectors.groupingBy(pair -> pair.getLeft().getSimpleName()));

        var postGroupedByName = postVersion.stream()
                .collect(Collectors.groupingBy(pair -> pair.getLeft().getSimpleName()));
        logger.info("Finished grouping by test method name");

        for (var entry : preGroupedByName.entrySet()) {
            String name = entry.getKey();
            List<MethodInvocation> preInvocations = entry.getValue().stream()
                    .map(Pair::getRight)
                    .collect(Collectors.toList());

            List<MethodInvocation> postInvocations = postGroupedByName.getOrDefault(name, new ArrayList<>()).stream()
                    .map(Pair::getRight)
                    .collect(Collectors.toList());

            matchedInvocations.add(Pair.of(preInvocations, postInvocations));
        }

        return matchedInvocations;
    }

    private List<Pair<MethodInvocation, MethodInvocation>> matchOnArgumentsAndOrder(
            List<MethodInvocation> preVersion, List<MethodInvocation> postVersion) {
        if (preVersion.size() == postVersion.size()) {
            List<Pair<MethodInvocation, MethodInvocation>> pairs = new ArrayList<>();
            for (int i = 0; i < preVersion.size(); i++) {
                MethodInvocation pre = preVersion.get(i);
                MethodInvocation post = postVersion.get(i);
                pairs.add(Pair.of(pre, post));
            }
            return pairs;
        } else {
            logger.error("Pre and post versions do not have the same number of invocations");
            return new ArrayList<>();
        }
    }
}
