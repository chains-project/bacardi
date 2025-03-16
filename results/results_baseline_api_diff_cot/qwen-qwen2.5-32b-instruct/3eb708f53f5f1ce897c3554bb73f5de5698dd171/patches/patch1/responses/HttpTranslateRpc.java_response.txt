package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HttpTranslateRpc implements TranslateRpc {

  private final TranslateOptions options;
  private final com.google.cloud.translate.v3.Translate translate;

  public HttpTranslateRpc(TranslateOptions options) {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpTransport transport = transportOptions.getHttpTransportFactory().create();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);
    this.options = options;
    this.translate = new com.google.cloud.translate.v3.Translate(transport, new JacksonFactory(), initializer);
  }

  private static TranslateException translate(IOException exception) {
    return new TranslateException(exception);
  }

  private GenericUrl buildTargetUrl(String path) {
    GenericUrl genericUrl = new GenericUrl(translate.getBaseUrl() + "v2/" + path);
    if (options.getApiKey() != null) {
      genericUrl.put("key", options.getApiKey());
    }
    return genericUrl;
  }

  @Override
  public List<List<Map<String, Double>>> detect(List<String> texts) {
    try {
      List<List<Map<String, Double>>> detections = translate.detect(texts).getDetections();
      return detections != null ? detections : ImmutableList.of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Map<String, String>> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      List<Map<String, String>> languages = translate.listSupportedLanguages().getLanguages();
      return languages != null ? languages : ImmutableList.of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Map<String, String>> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage = firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      List<Map<String, String>> translations = translate.translate(texts, targetLanguage).getTranslations();
      return Lists.transform(
          translations != null ? translations : ImmutableList.of(),
          new Function<Map<String, String>, Map<String, String>>() {
            @Override
            public Map<String, String> apply(Map<String, String> translationsResource) {
              // Since setDetectedSourceLanguage and getDetectedSourceLanguage are removed, we need to handle this differently.
              // Assuming the source language is known or can be inferred, we can set it directly.
              String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
              translationsResource.put("sourceLanguage", sourceLanguage);
              return translationsResource;
            }
          });
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}