package se.kth.Util;

public class Constants {

    public static final String MAVEN_LOG_FILE = "maven.log";

    public static final String BENCHMARK_PATH = "/home/kth/Documents/BUMP/breaking-updates/data/benchmark";

    public static final String JAVA_VERSION_INCOMPATIBILITY_FILE = "/home/kth/Documents/bacardi/gpt/bacardi/analysis/java_version_incompatibility.txt";

    public static final String PYTHON_SCRIPT = "/home/kth/Documents/bacardi/gpt/bacardi/llm/call_llm.py";


    /**
     * Path to the projects
     * for each project, there is a maven.log file and both version of the
     * dependency updated
     * folder name would be the commit hash
     * bd3ce213e2771c6ef7817c80818807a757d4e94a
     * |---maven.log
     * |---dependency-updated
     * |---original
     * |---updated
     * |-- tar file
     */
    public static final String PROJECTS_PATH = "maven.log";

}