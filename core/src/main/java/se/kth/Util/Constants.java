package se.kth.Util;

import io.github.cdimascio.dotenv.Dotenv;
import se.kth.model.PromptPipeline;

public class Constants {

        // Load the .env file
        private static final Dotenv dotenv = Dotenv.load();

        public static final String MAVEN_LOG_FILE = dotenv.get("MAVEN_LOG_FILE", "default.log");

        public static final String BENCHMARK_PATH = dotenv.get("BENCHMARK_PATH", "benchmark");

        public static final String JAVA_VERSION_INCOMPATIBILITY_FILE = dotenv.get("JAVA_VERSION_INCOMPATIBILITY_FILE",
                        System.getProperty("user.dir") + "/java_incompatibility.txt");

        public static final String PYTHON_SCRIPT = dotenv.get("PYTHON_SCRIPT",
                        System.getProperty("user.dir") + "python_script.py");

        public static final String PROJECTS_PATH = dotenv.get("PROJECTS_PATH",
                        System.getProperty("user.dir") + "/projects");;

        public static final String OUTPUT_PATH = dotenv.get("OUTPUT_PATH", System.getProperty("user.dir") + "/output");

        public static final String LLM = dotenv.get("LLM", "gtp4o-mini");

        public static final PromptPipeline PIPELINE = PromptPipeline
                        .fromString(dotenv.get("PIPELINE", PromptPipeline.BASELINE.toString()));

        public static final int MAX_ATTEMPTS = Integer.parseInt(dotenv.get("MAX_ATTEMPT", "3"));
}
