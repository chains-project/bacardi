package se.kth.rebuild.librariesio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Project(@JsonProperty("description") String description,
                      @JsonProperty("homepage") URL homepage,
                      @JsonProperty("repository_url") URL repositoryUrl,
                      @JsonProperty("versions") List<Version> versions) {
}
