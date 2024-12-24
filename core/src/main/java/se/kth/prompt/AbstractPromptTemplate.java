package se.kth.prompt;

import se.kth.model.PromptModel;

@lombok.Getter
@lombok.Setter
public abstract class AbstractPromptTemplate {

    protected PromptModel promptModel;

    public abstract String header();

    protected String apiDiff() {
        return "";
    }

    public abstract String classCode();

    public abstract String errorLog();

    public abstract String generatePrompt();


}
