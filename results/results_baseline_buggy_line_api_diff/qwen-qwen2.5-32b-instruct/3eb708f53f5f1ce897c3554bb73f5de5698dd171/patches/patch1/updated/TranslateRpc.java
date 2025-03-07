package com.google.cloud.translate.spi.v2;

import com.google.cloud.translate.DetectionsResourceItems;
import com.google.cloud.translate.LanguagesResource;
import com.google.cloud.translate.TranslationsResource;
import com.google.cloud.ServiceRpc;
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

  List<LanguagesResource> listSupportedLanguages(Map<Option, ?> optionMap);

  List<List<DetectionsResourceItems>> detect(List<String> texts);

  List<TranslationsResource> translate(List<String> texts, Map<Option, ?> optionMap);
}