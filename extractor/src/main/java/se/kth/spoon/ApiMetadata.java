package se.kth.spoon;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Getter
public class ApiMetadata {
    private final String name;
    private final Path path;

    public ApiMetadata(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    /**
     * List all classes from a jar file
     *
     * @return List of class names
     */
    public List<String> listAllClassFromJar() {

        try (JarFile jarFile = new JarFile(path.toFile())) {

            List<String> classNames = new ArrayList<>();

            Enumeration<JarEntry> e = jarFile.entries();

            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if (je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }
                // -6 because of .class
                String className = je.getName().substring(0, je.getName().length() - 6);
                className = className.replace('/', '.');
                classNames.add(className);
            }

            return classNames;
        } catch (IOException | SecurityException e) {
            throw new RuntimeException(e);
        }

    }


}
