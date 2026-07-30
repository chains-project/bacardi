package se.kth.scripts;

import se.kth.ApptainerBuild;
import se.kth.Util.BreakingUpdateProvider;
import se.kth.model.BreakingUpdate;
import se.kth.models.FailureCategory;

import java.nio.file.Path;
import java.util.List;

public class DependenciesExtractor {

    public static void main(String[] args) {
        String filePath = "/home/leonard/code/java/bump/data/benchmark/";
        String outputDirectory = "/home/leonard/tmp-output";
        String containerOutputDirectory = "/app";
        List<BreakingUpdate> testFailureUpdates =
                BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(filePath,
                        FailureCategory.TEST_FAILURE);

        ApptainerBuild containerBuild = new ApptainerBuild(false);
        String bind = "%s:%s".formatted(outputDirectory, containerOutputDirectory);

        for (BreakingUpdate update : testFailureUpdates) {
            extractSingleContainer(update, containerOutputDirectory, containerBuild, bind);
        }
    }

    private static void extractSingleContainer(BreakingUpdate update, String containerOutputDirectory,
                                                ApptainerBuild containerBuild, String bind) {
        try {
            String imageId = update.preCommitReproductionCommand.replace("docker run ", "");
            String outputTag = getOutputFileTagFromImageId(imageId);
            Path containerOutputPath = Path.of(containerOutputDirectory, outputTag + ".log");

            String command =
                    "mvn dependency:tree -DoutputType=dot -DoutputFile=%s -DappendOutput=true -am".formatted(containerOutputPath.toAbsolutePath().toString());

            containerBuild.ensureBaseMavenImageExists(imageId);
            String containerId = containerBuild.startSpinningContainer(imageId, List.of(bind));
            containerBuild.executeInContainer(containerId, "/bin/sh", "-c", command);
            containerBuild.removeContainer(containerId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String getOutputFileTagFromImageId(String imageId) {
        String[] split = imageId.split("/");
        return split[split.length - 1];
    }
}
