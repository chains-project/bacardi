package se.kth.Util;

import io.github.cdimascio.dotenv.Dotenv;

public class Constants {

    // Load the .env file
    private static final Dotenv dotenv = Dotenv.load();

    public static final String MAVEN_LOG_FILE = dotenv.get("MAVEN_LOG_FILE", "default.log");

    public static final String BENCHMARK_PATH = dotenv.get("BENCHMARK_PATH", "benchmark");

    public static final String JAVA_VERSION_INCOMPATIBILITY_FILE = dotenv.get("JAVA_VERSION_INCOMPATIBILITY_FILE", "/default/path/to/java_incompatibility.txt");

    public static final String PYTHON_SCRIPT = dotenv.get("PYTHON_SCRIPT", "/default/path/to/python_script.py");

    public static final String PROJECTS_PATH = "/Users/frank/Documents/Work/PHD/bacardi/projects";

}

