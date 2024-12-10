package se.kth.prompt;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GeneratePrompt {
    public static String callPythonScript(String scriptPath, String[] args) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Lee los errores del script de Python
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errors = new StringBuilder();

            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append(System.lineSeparator());
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Script ejecutado con éxito. Salida:");
                System.out.println(output.toString());
            } else {
                System.err.println("El script terminó con errores:");
                System.err.println(errors.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}