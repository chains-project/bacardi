package se.kth.rebuild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.model.UpdatedDependency;
import se.kth.rebuild.librariesio.LibrariesIO;
import se.kth.rebuild.librariesio.model.Project;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class RepoResolver {

    private final static Logger logger = LoggerFactory.getLogger(RepoResolver.class);

    private final LibrariesIO librariesIO;

    public RepoResolver(LibrariesIO librariesIO) {
        this.librariesIO = librariesIO;
    }

    public Optional<URL> getGitUrl(UpdatedDependency dependency) {
        Optional<URL> bumpUrl = this.tryGetRepoUrl(dependency);
        if (bumpUrl.isPresent()) {
            URL gitUrl = this.toGitUrl(bumpUrl.get());
            return Optional.of(gitUrl);
        } else {
            Optional<URL> repoUrl = this.tryGetRepoUrlLibrariesIO(dependency);
            if (repoUrl.isPresent()) {
                URL gitUrl = this.toGitUrl(repoUrl.get());
                return Optional.of(gitUrl);
            } else {
                return Optional.empty();
            }
        }
    }

    private Optional<URL> tryGetRepoUrl(UpdatedDependency updatedDependency) {
        String compareLink = updatedDependency.githubCompareLink;
        if (compareLink.contains("/compare/")) {
            String[] split = compareLink.split("/compare/");
            String repoUrl = split[0];
            try {
                URL githubRepoUrl = new URL(repoUrl);
                return Optional.of(githubRepoUrl);
            } catch (MalformedURLException e) {
                logger.warn("Couldn't extract repository url from \"{}\"", compareLink);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<URL> tryGetRepoUrlLibrariesIO(UpdatedDependency updatedDependency) {
        try {
            Project project = this.librariesIO.getProject(updatedDependency);
            logger.info("Got project for " + updatedDependency.dependencyArtifactID);
            return project != null ? Optional.of(project.repositoryUrl()) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private URL toGitUrl(URL url) {
        String urlString = url.toString();
        try {
            return new URL(urlString + ".git");
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
            return null;
        }
    }
}
