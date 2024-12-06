package se.kth.spoon;

import lombok.Getter;

import java.nio.file.Path;

@Getter
public class ApiMetadata {
    private final String name;
    private final Path path;

    public ApiMetadata(String name, Path path) {
        this.name = name;
        this.path = path;
    }

}
