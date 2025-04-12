package rq3;

/**
 * This class represents the information about a breaking change.
 * @param lineNumber
 * @param codeLine
 * @param classPath
 */
public record BreakingInfo(
        int lineNumber,
        String codeLine,
        String classPath
) {}
