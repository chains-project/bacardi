package se.kth.scripts;

import org.apache.commons.lang3.tuple.ImmutablePair;
import se.kth.Util.FileUtils;
import se.kth.Util.JsonUtils;
import se.kth.extractor.CausingConstruct;
import se.kth.model.BreakingUpdate;
import se.kth.utils.Config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CausingConstructReader {
    public static void main(String[] args) {
        Path resultFilePath = Config.getResourcesDir().resolve("causing-constructs.json");
        List<BreakingUpdate> bump = readBump();
        HashMap<String, HashMap<String, List<String>>> result = JsonUtils.readFromFile(resultFilePath,
                HashMap.class);
        List<Map.Entry<String, HashMap<String, List<String>>>> nonEmpty = result.entrySet().stream()
                .filter(stringHashMapEntry -> !stringHashMapEntry.getValue().isEmpty())
                .toList();

        List<String> allEnums = result.values().stream()
                .flatMap(innerMap -> innerMap.values().stream())
                .flatMap(List::stream)
                .collect(Collectors.toList());

        Map<String, List<String>> groupedConstructs = allEnums.stream()
                .collect(Collectors.groupingBy(e -> e));
        List<String> all = result.values().stream()
                .flatMap(stringListHashMap -> stringListHashMap.values().stream())
                .flatMap(List::stream)
                .toList();
        Map<String, List<String>> grouped = all.stream()
                .collect(Collectors.groupingBy(s -> s));

        List<BreakingUpdate> collectedBumpProjects = bump.stream()
                .filter(breakingUpdate -> result.keySet().contains(breakingUpdate.breakingCommit) && !result.get(breakingUpdate.breakingCommit).isEmpty())
                .toList();

        var groupedBumpProjects = collectedBumpProjects.stream()
                .collect(Collectors.groupingBy(breakingUpdate -> breakingUpdate.project));

        var groupedUpdates = collectedBumpProjects.stream()
                .collect(Collectors.groupingBy(breakingUpdate -> breakingUpdate.updatedDependency.dependencyArtifactID));

        var differentProjects = bump.stream()
                .filter(breakingUpdate -> result.keySet().contains(breakingUpdate.breakingCommit))
                .collect(Collectors.groupingBy(breakingUpdate -> breakingUpdate.project));

        var differentUpdates = bump.stream()
                .filter(breakingUpdate -> result.keySet().contains(breakingUpdate.breakingCommit))
                .collect(Collectors.groupingBy(breakingUpdate -> breakingUpdate.updatedDependency.dependencyArtifactID));

        var allTestCases = nonEmpty.stream()
                .flatMap(stringHashMapEntry -> stringHashMapEntry.getValue().values().stream())
                .toList();

        List<ImmutablePair<CausingConstruct, List<List<String>>>> involvedForEachConstruct =
                Arrays.stream(CausingConstruct.values())
                        .map(causingConstruct -> new ImmutablePair<CausingConstruct, List<List<String>>>(causingConstruct,
                                allTestCases.stream()
                                        .filter(strings -> strings.contains(causingConstruct.toString()))
                                        .toList()))
                        .toList();


        System.out.println("Analyzed projects (clients): " + groupedBumpProjects.size());
        System.out.println("Analyzed updates (dependencies): " + groupedUpdates.size());
        System.out.println("Analyzed combinations: " + nonEmpty.size());
        System.out.println("contained in BUMP:");
        System.out.println("Different projects (clients): " + differentProjects.size());
        System.out.println("Different updates (dependencies): " + differentUpdates.size());
        involvedForEachConstruct.forEach(immutablePair -> System.out.println(immutablePair.left.toString() + " " +
                "involved in updates: " + immutablePair.right.size()));
        System.out.println("Total test cases: " + allTestCases.size());
    }

    private static List<BreakingUpdate> readBump() {
        Path bumpDir = Config.getBumpDir();
        return FileUtils.getFilesInDirectory(bumpDir.toString()).stream()
                .map(file -> JsonUtils.readFromFile(file.toPath(), BreakingUpdate.class))
                .toList();
    }
}
