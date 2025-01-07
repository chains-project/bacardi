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

    private Git git;

    public GitManager(File repo) {
        this.repo = repo;
    }


    public Git checkRepoStatus() {
        try (Repository repository = new FileRepositoryBuilder()
                .setGitDir(repo)
                .readEnvironment()
                .findGitDir()
                .build()) {

            git = new Git(repository);
            if (repository.isBare()) {
                git = Git.init().setBare(false).setDirectory(repo).call();
            }

            // Check if repository head is null
            if (repository.resolve("HEAD") == null) {
                log.error("Repository HEAD is null");
                git.commit().setMessage("Initial commit for breaking dependency update").setSign(false).call();
            }

            newBranch(Constants.BRANCH_ORIGINAL_STATUS);

            return git;

        } catch (IOException | GitAPIException e) {
            log.error("Error opening repository", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new branch in the Git repository.
     *
     * @return true if the branch was successfully created, false otherwise.
     * @throws RuntimeException if there is an error opening the repository or creating the branch.
     */
    public boolean newBranch(String branchName) {

        try {

            if (git.branchList().call().stream().anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName))) {

                if (branchName.equals(Constants.BRANCH_ORIGINAL_STATUS)) {
                    git.checkout().setName(branchName).call();
                    git.branchList().call().forEach(ref -> {
                        if (!ref.getName().equals("refs/heads/" + "original_status")) {
                            try {
                                git.branchDelete().setBranchNames(ref.getName()).setForce(true).call();
                            } catch (GitAPIException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

                    return true;
                }
                log.info("Branch {} already exists", branchName);
                // delete branch
                return false;
//                git.branchDelete().setBranchNames(branchName).setForce(true).call();
            }

            //Add all changes in the working branch
            git.add().addFilepattern(".").call();

            // Commit the changes
            git.commit().setMessage("Committing changes to create branch " + branchName).setSign(false).call();

            // Create a new branch with the given name and checkout
            git.checkout().setCreateBranch(true).setName(branchName).call();

            log.info("Branch {} created", branchName);
            return true;

        } catch (GitAPIException e) {
            log.error("Error opening repository", e);
            throw new RuntimeException(e);
        }
    }

    public boolean commitAllChanges(String message) {

        if (message == null || message.isEmpty()) {
            message = "Committing changes";
        }

        try {
            git.add().addFilepattern(".").call();
            git.commit().setMessage(message).setSign(false).call();
            return true;
        } catch (GitAPIException e) {
            log.error("Error committing changes", e);
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
