package se.kth.prompt;

public enum LLMType {
    GEMINI("gemini"),
    GPT4("gPT-4"),
    LLAMA("llama"),
    MIXTRAL("mixtral");

    private final String displayName;

    LLMType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}