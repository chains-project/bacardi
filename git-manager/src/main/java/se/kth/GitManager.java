package se.kth;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class GitManager {

    private final File repo;

    private final Logger log = LoggerFactory.getLogger(GitManager.class);

    private String branchName = "Fixing-breaking-dependency-update";

    public GitManager(File repo) {
        this.repo = repo;
    }


    /**
 * Creates a new branch in the Git repository.
 *
 * @return true if the branch was successfully created, false otherwise.
 * @throws RuntimeException if there is an error opening the repository or creating the branch.
 */
public boolean newBranch() {
    try {
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(repo)
                .readEnvironment()
                .findGitDir()
                .build();

        Git git = new Git(repository);
        if (repository.isBare()) {
            git = Git.init().setBare(false).setDirectory(repo).call();
        }

        // Check if repository head is null
        if (repository.resolve("HEAD") == null) {
            log.error("Repository HEAD is null");
            git.commit().setMessage("Initial commit for breaking dependency update").call();
        }

        // Check if branch already exists
        if (git.branchList().call().stream().anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName))) {
            branchName = branchName + System.currentTimeMillis();
        }
        // Create a new branch with the given name and checkout
        git.checkout().setCreateBranch(true).setName(branchName).call();

        log.info("Branch {} created", branchName);
        return true;

    } catch (IOException | GitAPIException e) {
        log.error("Error opening repository", e);
        throw new RuntimeException(e);
    }
}


    public boolean deleteBranch(String branchName) {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(repo)
                    .readEnvironment()
                    .findGitDir()
                    .build();

            Git git = new Git(repository);

            git.branchDelete().setBranchNames(branchName).call();
            log.info("Branch {} deleted", branchName);
            return true;

        } catch (IOException | GitAPIException e) {
            log.error("Error deleting branch", e);
            throw new RuntimeException(e);
        }
    }

}
