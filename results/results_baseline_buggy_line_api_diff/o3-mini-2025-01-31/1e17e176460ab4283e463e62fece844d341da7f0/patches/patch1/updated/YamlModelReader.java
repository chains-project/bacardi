package org.sonatype.maven.polyglot.yaml;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.io.ModelReaderSupport;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

@Component(role = ModelReader.class, hint = "yaml")
public class YamlModelReader extends ModelReaderSupport {
  private final Yaml yaml;

  public YamlModelReader() {
    ModelConstructor constructor = new ModelConstructor();
    DumperOptions dumperOptions = new DumperOptions();
    Representer representer = new Representer(dumperOptions);
    yaml = new Yaml(constructor, representer, dumperOptions, new ModelResolver());
  }

  public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
    if (input == null) {
      throw new IllegalArgumentException("YAML Reader is null.");
    }

    return (Model) yaml.load(input);
  }
}