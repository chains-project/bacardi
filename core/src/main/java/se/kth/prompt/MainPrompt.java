package se.kth.prompt;

import se.kth.model.SetupPipeline;

import java.nio.file.Path;
import java.util.Set;

import static se.kth.prompt.GeneratePrompt.callPythonScript;

public class MainPrompt {
    public static void main(String[] args) {
        String scriptPath = "/Users/frank/Documents/Work/PHD/bacardi/bacardi/llm/call_llm.py";

        String outputs = callPythonScript(scriptPath, new String[]{});
//        System.out.println(outputs);


        GeneratePrompt extract = new GeneratePrompt();
        String content = extract.extractContentFromModelResponse(outputs);

        SetupPipeline setupPipeline = new SetupPipeline();
        setupPipeline.setOutPutPatchFolder(Path.of("/"));
        StoreInfo storeInfo = new StoreInfo(setupPipeline);


        System.out.println("Content: ");
        System.out.println(content);

    }

}
