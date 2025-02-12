package se.kth.prompt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.model.PromptModel;

@lombok.Getter
@lombok.Setter
public abstract class AbstractPromptTemplate {

    private static final Logger log = LoggerFactory.getLogger(AbstractPromptTemplate.class);

    protected PromptModel promptModel;

    public abstract String header();

    protected String apiDiff() {
        return "";
    }

    public abstract String classCode();

    public abstract String errorLog();

    public abstract String generatePrompt();

    public String parseLLMResponce(String response) {
        String pattern = "```java(.*?)```";
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regex.matcher(response);

        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            log.error("Error extracting content from the model response");
            return null;
        }

    };

}
