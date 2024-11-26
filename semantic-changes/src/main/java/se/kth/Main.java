package se.kth;

import se.kth.Util.BreakingUpdateProvider;
import se.kth.model.BreakingUpdate;
import se.kth.model.UpdatedDependency;
import se.kth.models.FailureCategory;
import se.kth.rebuild.RepoResolver;
import se.kth.rebuild.librariesio.LibrariesIO;
import se.kth.rebuild.librariesio.Rebuilder;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        Path bumpDir = getBumpDir();
        List<BreakingUpdate> breakingUpdates =
                BreakingUpdateProvider.getBreakingUpdatesFromResourcesByCategory(bumpDir.toString(),
                        FailureCategory.TEST_FAILURE);
        LibrariesIO librariesIO = new LibrariesIO(args[0]);
        RepoResolver repoResolver = new RepoResolver(librariesIO);
        DockerBuild dockerClient = new DockerBuild(false);
        Rebuilder rebuilder = new Rebuilder(dockerClient);

        breakingUpdates.forEach(breakingUpdate -> {
            if (breakingUpdate.updatedDependency.dependencyArtifactID.contains("junit")) {
                System.out.println("Skipping " + breakingUpdate.updatedDependency.dependencyArtifactID);
                return;
            }
            UpdatedDependency updatedDependency = breakingUpdate.updatedDependency;
            Optional<URL> repoUrl = repoResolver.getGitUrl(updatedDependency);
            repoUrl.ifPresent(url -> rebuilder.rebuildProject(url, updatedDependency.previousVersion));
        });
    }

    public static Path getBumpDir() {
        Path currentDir = Paths.get("").toAbsolutePath();
        Path bumpRelativePath = Path.of("bump", "data", "benchmark");
        return currentDir.resolveSibling(bumpRelativePath);
    }
}