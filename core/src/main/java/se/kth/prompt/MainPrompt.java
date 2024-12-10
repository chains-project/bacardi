package se.kth.prompt;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static se.kth.prompt.GeneratePrompt.callPythonScript;

public class MainPrompt {
    public static void main(String[] args) {
        String scriptPath = "/Users/frank/Documents/Work/PHD/bacardi/bacardi/llm/call_llm.py";

        String outputs = callPythonScript(scriptPath, new String[]{});
//        System.out.println(outputs);


        Float.intBitsToFloat(5);
    }

}
