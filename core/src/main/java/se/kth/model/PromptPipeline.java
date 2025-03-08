package se.kth.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.prompt.GeneratePrompt;

public enum PromptPipeline {
    BASELINE,
    BASELINE_ANTHROPIC,
    BASELINE_ANTHROPIC_BUGGY,
    BASELINE_API_DIFF,
    BASELINE_BUGGY_LINE,
    BASELINE_API_DIFF_BUGGY,
    BASELINE_COT_API_DIFF,
    BASELINE_COT_API_DIFF_BUGGY,
    FIX_YOU,
    ADVANCED;

    public static PromptPipeline fromString(String pipeline) {

        Logger log = LoggerFactory.getLogger(GeneratePrompt.class);

        return switch (pipeline) {
            case "BASELINE" -> BASELINE;
            case "BASELINE_ANTHROPIC" -> BASELINE_ANTHROPIC;
            case "BASELINE_API_DIFF" -> BASELINE_API_DIFF;
            case "BASELINE_API_DIFF_BUGGY" -> BASELINE_API_DIFF_BUGGY;
            case "BASELINE_BUGGY_LINE" -> BASELINE_BUGGY_LINE;
            case "BASELINE_ANTHROPIC_BUGGY" -> BASELINE_ANTHROPIC_BUGGY;
            case "BASELINE_COT_API_DIFF" -> BASELINE_COT_API_DIFF;
            case "BASELINE_COT_API_DIFF_BUGGY" -> BASELINE_COT_API_DIFF_BUGGY;
            case "ADVANCED" -> ADVANCED;
            case "FIX_YOU" -> FIX_YOU;
            default -> {
                log.error("Invalid pipeline: {}", pipeline);
                throw new IllegalArgumentException("Invalid pipeline: " + pipeline);
            }
        };
    }

}
