package com.google.cloud.translate.spi.v2;

import java.util.List;
import java.util.Map;

public interface TranslateRpc extends ServiceRpc {

  enum Option {
    SOURCE_LANGUAGE("source"),
    TARGET_LANGUAGE("target"),
    MODEL("model"),
    FORMAT("format");

    private final String value;

    Option(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }

    @SuppressWarnings("unchecked")
    <T> T get(Map<Option, ?> options) {
      return (T) options.get(this);
    }

    String getString(Map<Option, ?> options) {
      return get(options);
    }
  }

  /**
   * Returns a list of the languages supported by Google Translation.
   *
   * @param optionMap options to listing language translations
   */
  List<String> listSupportedLanguages(Map<Option, ?> optionMap);

  /**
   * Detects the language of the provided texts.
   *
   * @param texts the texts to translate
   * @return a list of lists of detections, one list of detections for each provided text, in order
   */
  List<List<String>> detect(List<String> texts);

  /**
   * Translates the provided texts.
   *
   * @param texts the texts to translate
   * @param optionMap options to text translation
   * @return a list of resources containing translation information, in the same order of the
   *     provided texts
   */
  List<String> translate(List<String> texts, Map<Option, ?> optionMap);
}