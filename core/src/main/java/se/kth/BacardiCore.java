package se.kth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.Util.LogUtils;
import se.kth.Util.StoreInfo;
import se.kth.direct_failures.RepairDirectFailures;
import se.kth.failure_detection.DetectedFileWithErrors;
import se.kth.java_version.RepairJavaVersionIncompatibility;
import se.kth.model.PromptModel;
import se.kth.model.PromptPipeline;
import se.kth.model.SetupPipeline;
import se.kth.models.*;
import se.kth.prompt.GeneratePrompt;
import se.kth.wError.RepairWError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static se.kth.Util.Constants.*;
import static se.kth.Util.FileUtils.getAbsolutePath;

public class BacardiCore {

    private static final Logger log = LoggerFactory.getLogger(BacardiCore.class);
    private final Path project;
    private final Path logFile;

    private final FailureCategoryExtract failureCategoryExtract;
    private FailureCategory previousFailureCategory;
    private FailureCategory failureCategory;
    private SetupPipeline setupPipeline;
    private PromptPipeline promptPipeline;

    private Result result;

    Boolean isBump = false;
    String actualImage;

    public BacardiCore(Path project, Path logFile, FailureCategoryExtract failureCategoryExtract, Boolean isBump) {
        this.project = Objects.requireNonNull(project, "Project path cannot be null");
        this.logFile = Objects.requireNonNull(logFile, "Log file path cannot be null");
        this.failureCategoryExtract = Objects.requireNonNull(failureCategoryExtract, "Failure category cannot be null");
        this.isBump = isBump;
        verify();
    }

    public BacardiCore(SetupPipeline setupPipeline, FailureCategoryExtract failureCategoryExtract, Boolean isBump) {
        this.project = Objects.requireNonNull(setupPipeline.getClientFolder(), "Project path cannot be null");
        this.logFile = Objects.requireNonNull(setupPipeline.getLogFilePath(), "Log file path cannot be null");
        this.failureCategoryExtract = Objects.requireNonNull(failureCategoryExtract, "Failure category cannot be null");
        this.setupPipeline = Objects.requireNonNull(setupPipeline, "Check setup pipeline");
        this.isBump = isBump;
        switch (PIPELINE) {
            case BASELINE:
                promptPipeline = PromptPipeline.BASELINE;
                break;
            case BASELINE_BUGGY_LINE:
                promptPipeline = PromptPipeline.BASELINE_BUGGY_LINE;
                break;
            case BASELINE_ANTHROPIC:
                promptPipeline = PromptPipeline.BASELINE_ANTHROPIC;
                break;
            case BASELINE_ANTHROPIC_BUGGY:
                promptPipeline = PromptPipeline.BASELINE_ANTHROPIC_BUGGY;
                break;
            case FIX_YOU:
                promptPipeline = PromptPipeline.FIX_YOU;
                break;
            case BASELINE_API_DIFF:
                promptPipeline = PromptPipeline.BASELINE_API_DIFF;
                break;
            default:
                promptPipeline = PromptPipeline.BASELINE;
                break;
        }
        verify();
    }

    public void verify() {
        if (!Files.exists(project)) {
            throw new IllegalArgumentException("Project path does not exist.");
        }
        if (!Files.exists(logFile)) {
            throw new IllegalArgumentException("Log file path does not exist.");
        }
    }

    public Result analyze() {

        failureCategory = failureCategoryExtract.getFailureCategory(setupPipeline.getLogFilePath().toFile());
        // Result value for each attempt
        result = new Result(failureCategory);

        int attempts = 1;
        StoreInfo storeInfo = new StoreInfo(setupPipeline, true);

        while (failureCategory != FailureCategory.BUILD_SUCCESS && attempts <= MAX_ATTEMPTS) {

            // Check if the project is a git repository
            GitManager gitManager = new GitManager(project.toFile());
            // Check the status of the repository and create a new branch for the original
            // status

            gitManager.checkRepoStatus();

            switch (failureCategory) {
                case JAVA_VERSION_FAILURE:
                    log.info("Java version failure detected.");
                    failureCategory = repairJavaVersionIncompatibility(gitManager);
                    break;
                case TEST_FAILURE:
                    log.info("Test failure detected.");
                    break;
                case WERROR_FAILURE:
                    log.info("Werror failure detected.");
                    failureCategory = repairWErrorIncompatibility(gitManager);
                    break;
                case COMPILATION_FAILURE:
                    log.info("Compilation failure detected.");
                    failureCategory = repairDirectCompilationFailure(gitManager, storeInfo);
                    break;
                case DEPENDENCY_LOCK_FAILURE:
                    log.info("Dependency lock failure detected.");
                    break;
                case DEPENDENCY_RESOLUTION_FAILURE:
                    log.info("Dependency resolution failure detected.");
                    break;
                case ENFORCER_FAILURE:
                    log.info("Enforcer failure detected.");
                    break;
                case NOT_REPAIRED:
                    log.info("Not repaired.");
                    attempts = MAX_ATTEMPTS + 1;
                    break;
                default:
                    attempts = MAX_ATTEMPTS + 1;
                    log.info("Unknown failure category.");
            }

            Attempt attempt = new Attempt(attempts, failureCategory, storeInfo.getPatchFolder().toString(), failureCategory == FailureCategory.BUILD_SUCCESS);

            log.info("Attempt: {}", attempt);
            result.getAttempts().add(attempt);
            // number of attempts to repair the failure
            attempts++;
        }

        if (failureCategory == FailureCategory.BUILD_SUCCESS) {
            // DockerBuild.deleteImage(actualImage);
            log.info("Build success in attempt: {}", attempts - 1);
        }
        if (failureCategory == FailureCategory.NOT_REPAIRED) {
            log.info("Not repaired in attempt: {}", attempts - 1);
            return result;
        }

        return result;
    }

    /**
     * Repairs direct compilation failures.
     *
     * @param gitManager the Git manager to handle repository operations
     * @return the new failure category after attempting the repair
     */
    private FailureCategory repairDirectCompilationFailure(GitManager gitManager, StoreInfo storeInfo) {
        // checking if the previous failure category is different from the current
        // failure category and create a new branch
        if (previousFailureCategory != failureCategory) {
            previousFailureCategory = failureCategory;
            gitManager.newBranch(Constants.BRANCH_DIRECT_COMPILATION_FAILURE + "_%s".formatted(result.getAttempts().size()));
        }
        DockerBuild dockerBuild = setupPipeline.getDockerBuild();
        AtomicBoolean errorModelResponse = new AtomicBoolean(false);

        // Ensure the base Maven image exists
        try {
            dockerBuild.ensureBaseMavenImageExists(setupPipeline.getDockerImage());
            log.info(setupPipeline.getDockerImage() + "Base Maven image exists.");
        } catch (InterruptedException e) {
            log.error("Error ensuring base maven image exists.", e);
            throw new RuntimeException(e);
        }

        Path project = setupPipeline.getClientFolder();
        Path logFile = setupPipeline.getLogFilePath();

        // Repair with llm
        RepairDirectFailures repairDirectFailures = new RepairDirectFailures(setupPipeline.getDockerBuild(), setupPipeline);

        ArrayList<Boolean> isDifferent = new ArrayList<>();
        FailureCategory category;
        try {
            Map<String, Set<DetectedFileWithErrors>> listOfFilesWithErrors = getListOfFilesWithErrors(repairDirectFailures);

            if (listOfFilesWithErrors.isEmpty()) {
                log.info("No constructs found in the direct compilation failure.");
                // try to get failures from indirect dependencies or conflicts between
                // dependencies
                return FailureCategory.NOT_REPAIRED;

            } else {
                log.info("Constructs found in the direct compilation failure: {}", listOfFilesWithErrors.size());

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                storeInfo.storeFilesErrors("prefix", listOfFilesWithErrors);

                for (Map.Entry<String, Set<DetectedFileWithErrors>> entry : listOfFilesWithErrors.entrySet()) {


                    String key = entry.getKey();
                    Set<DetectedFileWithErrors> value = entry.getValue();
                    log.info("File: {}", key);

                    if (value.isEmpty()) {
                        log.info("No errors found for: {}", key);
                    } else {

                        if (PIPELINE.equals(PromptPipeline.BASELINE_API_DIFF) &&
                                value.stream().filter(f -> !f.getApiChanges().isEmpty()).toList().isEmpty()) {
                            return FailureCategory.NOT_APIDIFF;

                        }

                        // if there are errors, generate a prompt for the file and execute the repair
                        String absolutePathToBuggyClass = getAbsolutePath(setupPipeline, key);
                        String fileName = key.substring(key.lastIndexOf("/") + 1);
                        // create all structure for save information

                        GeneratePrompt generatePrompt = new GeneratePrompt(promptPipeline, new PromptModel(absolutePathToBuggyClass, value, setupPipeline.getLibraryName(), setupPipeline.getBaseVersion(), setupPipeline.getNewVersion()));
                        String prompt = generatePrompt.generatePrompt();
                        log.info("Waiting for response...");
                        // save the prompt to a file for each file with errors
                        try {
                            Path promptPath = storeInfo.copyContentToFile("prompts/%s_prompt.txt".formatted(fileName), prompt);
                            String model_response = generatePrompt.callPythonScript(PYTHON_SCRIPT, promptPath);
//                            String model_response = "response";
                            // save model model_response to a file
                            storeInfo.copyContentToFile("responses/%s_model_response.txt".formatted(fileName), model_response);
                            String onlyCodeResponse = generatePrompt.extractContentFromModelResponse(model_response);
                            storeInfo.copyContentToFile("responses/%s_response.txt".formatted(fileName), onlyCodeResponse);
                            // save the updated file
                            Path updatedFile = storeInfo.copyContentToFile("updated/%s".formatted(fileName), onlyCodeResponse);
                            Path target = Path.of(absolutePathToBuggyClass);
                            Path originalFile = storeInfo.copyContentToFile("original/%s".formatted(fileName), Files.readString(target));
                            // execute the diff command
                            boolean isDiff = storeInfo.executeDiffCommand(originalFile.toAbsolutePath().toString(), updatedFile.toAbsolutePath().toString(), storeInfo.getPatchFolder().resolve("diffs/%s_diff.txt".formatted(fileName)));
                            isDifferent.add(isDiff);
                            // replace original file with updated file
                            if (isDiff) {
                                Files.copy(updatedFile, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception e) {
                            errorModelResponse.set(true);
                            log.error("Error saving prompt to file. {}", e.getMessage());
                            return FailureCategory.ERROR_MODEL_RESPONSE;
                        }


                    }

                }

                if (isDifferent.contains(true)) {
                    gitManager.commitAllChanges("Direct compilation failure repair attempt %s".formatted(result.getAttempts().size()));
                    // copy the file to docker image
                    try {
                        String dockerImage = dockerBuild.copyFolderToDockerImage(setupPipeline.getDockerImage(), setupPipeline.getClientFolder().toString());
                        setupPipeline.setDockerImage(dockerImage);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
//                     reproduce the build
                    Path logFilePath = storeInfo.getPatchFolder().resolve("output.log");
                    dockerBuild.reproduce(setupPipeline.getDockerImage(), FailureCategory.COMPILATION_FAILURE, setupPipeline.getClientFolder(), logFilePath);
                    setupPipeline.setLogFilePath(logFilePath);
                    RepairDirectFailures rebuildDirectFailures = new RepairDirectFailures(setupPipeline.getDockerBuild(), setupPipeline);
                    Map<String, Set<DetectedFileWithErrors>> listOfPostFixFilesWithErrors = getListOfFilesWithErrors(rebuildDirectFailures);
                    StoreInfo postFixstoreInfo = new StoreInfo(setupPipeline, storeInfo.getPatchFolder());
                    postFixstoreInfo.storeFilesErrors("postfix", listOfPostFixFilesWithErrors);

                } else {
                    // no changes were made
                    return FailureCategory.NOT_REPAIRED;
                }
                // Check and try dependency resolution conflicts
                category = failureCategoryExtract.getFailureCategory(setupPipeline.getLogFilePath().toFile());

                return category;
            }

        } catch (IOException e) {
            log.error("Error repairing direct compilation failure.", e);
            throw new RuntimeException(e);
        }

    }

    public Map<String, Set<DetectedFileWithErrors>> getListOfFilesWithErrors(RepairDirectFailures repairDirectFailures) throws IOException {

        PromptPipeline promptPipeLine = PIPELINE;

        return switch (promptPipeLine) {
            case BASELINE, BASELINE_ANTHROPIC, FIX_YOU -> repairDirectFailures.basePipeLine();
            case BASELINE_BUGGY_LINE, BASELINE_ANTHROPIC_BUGGY -> repairDirectFailures.buggyLinePipeLine();
            case BASELINE_API_DIFF -> repairDirectFailures.extractConstructsFromDirectFailures();
            default -> throw new IllegalStateException("Unexpected value: " + promptPipeLine);
        };
    }

    private FailureCategory repairJavaVersionIncompatibility(GitManager gitManager) {

        // Create a branch for the java version incompatibility repair
        gitManager.newBranch(Constants.BRANCH_JAVA_VERSION_INCOMPATIBILITY);

        JavaVersionInformation javaVersionInformation = new JavaVersionInformation(setupPipeline.getLogFilePath().toFile());
        JavaVersionInfo javaVersionInfo = javaVersionInformation.analyse(setupPipeline.getLogFilePath().toString(), project.toAbsolutePath().toString());

        JavaVersionIncompatibility incompatibility = javaVersionInfo.getIncompatibility();
        String newJavaVersion = javaVersionInfo.getIncompatibility().mapVersions(incompatibility.wrongVersion());

        LogUtils.logWithBox(log, "Starting Java version incompatibility repair.");

        RepairJavaVersionIncompatibility repairJavaVersionIncompatibility = new RepairJavaVersionIncompatibility(javaVersionInfo, project, isBump);

        actualImage = repairJavaVersionIncompatibility.repair(setupPipeline);
        setupPipeline.setDockerImage(actualImage);

        List<YamlInfo> javaVersions = javaVersionInfo.getJavaInWorkflowFiles();

        Path logFile = project.resolve("output.log".formatted(result.getAttempts().size()));

        // check if the new failure category is success
        FailureCategory newFailureCategory = failureCategoryExtract.getFailureCategory(logFile.toFile());

        // if (newFailureCategory.equals(failureCategoryExtract.)newFailureCategory ==
        // FailureCategory.BUILD_SUCCESS) {
        repairJavaVersionIncompatibility.updateJavaVersions(project.toString(), 17);
        // }

        gitManager.commitAllChanges("Java version incompatibility repair");

        return newFailureCategory;

    }

    private FailureCategory repairWErrorIncompatibility(GitManager gitManager) {
        // Create a branch for the werror repair
        if (previousFailureCategory != failureCategory) {
            gitManager.newBranch(Constants.BRANCH_WERROR);
        }

        // get Docker image in case of bump
        /*
         * modify the version to get the docker image from bump and not from the project
         */

        try {
            setupPipeline.getDockerBuild().ensureBaseMavenImageExists(setupPipeline.getDockerImage());
        } catch (InterruptedException e) {
            log.error("Error ensuring base maven image exists.", e);
            throw new RuntimeException(e);
        }

        LogUtils.logWithBox(log, "Starting Werror incompatibility repair.");

        Path logFile = setupPipeline.getLogFilePath();

        try {

            WerrorInformation werrorInformation = new WerrorInformation(setupPipeline.getLogFilePath().toFile());

            WerrorInfo werrorInfo = werrorInformation.analyzeWerror(setupPipeline.getClientFolder().toString());

            RepairWError repairWError = new RepairWError(project, isBump, setupPipeline.getDockerImage(), setupPipeline);

            if (repairWError.isWerrorJavaVersionIncompatibilityError(logFile.toAbsolutePath().toString())) {
                // find all pom files with werror
                repairWError.replaceJavaVersion(project.resolve("pom.xml").toString(), 17);
            }

            repairWError.reproduce();
            // update the setup pipeline with the new log file and all information
            setupPipeline = repairWError.getSetupPipeline();

            log.info("Werror info: {}", werrorInfo);

        } catch (Exception e) {
            log.error("Error extracting warning lines.", e);
        }

        gitManager.commitAllChanges("Werror incompatibility repair");
        return failureCategoryExtract.getFailureCategory(logFile.toFile());

    }

}