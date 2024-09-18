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

    private File repo;

    // Logger
    private final Logger log = LoggerFactory.getLogger(GitManager.class);


    public GitManager(File repo) {
        this.repo = repo;
    }


    public boolean newBranch(String branchName) {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(repo)
                    .readEnvironment()
                    .findGitDir()
                    .build();

            Git git = Git.init().setBare(false).setDirectory(repo).call();

            if (repository.resolve("HEAD") == null) {
                log.error("Repository is empty");
                git.commit().setMessage("Initial commit").call();


            }

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

    public static void main(String[] args) {
        File repo = new File("/Users/frank/Downloads/tmp/sbom.exe");
        GitManager gitManager = new GitManager(repo);
        gitManager.newBranch("testdfgdfg");

    }

}
