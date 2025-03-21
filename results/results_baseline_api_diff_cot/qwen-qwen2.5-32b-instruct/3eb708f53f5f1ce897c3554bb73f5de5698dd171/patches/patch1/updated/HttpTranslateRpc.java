package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.v3.Translate;
import com.google.api.services.translate.v3.model.DetectLanguageRequest;
import com.google.api.services.translate.v3.model.DetectLanguageResponse;
import com.google.api.services.translate.v3.model.Glossary;
import com.google.api.services.translate.v3.model.ListGlossariesResponse;
import com.google.api.services.translate.v3.model.TranslateTextRequest;
import com.google.api.services.translate.v3.model.TranslateTextResponse;
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
  private final Translate translate;

  public HttpTranslateRpc(TranslateOptions options) {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpTransport transport = transportOptions.getHttpTransportFactory().create();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);
    this.options = options;
    translate =
        new Translate.Builder(transport, new JacksonFactory(), initializer)
            .setRootUrl(options.getHost())
            .setApplicationName(options.getApplicationName())
            .build();
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
  public List<List<String>> detect(List<String> texts) {
    try {
      DetectLanguageRequest request = new DetectLanguageRequest().setTexts(texts);
      DetectLanguageResponse response = translate.projects().locations().detectLanguage(request).setParent("projects/" + options.getProjectId()).execute();
      return response.getLanguages();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Glossary> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      String targetLanguage = firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      ListGlossariesResponse response = translate.projects().locations().glossaries().list("projects/" + options.getProjectId() + "/locations/global").execute();
      return response.getGlossaries();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<String> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage = firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
      TranslateTextRequest request = new TranslateTextRequest().setTargetLanguage(targetLanguage).setSourceLanguage(sourceLanguage).setTexts(texts);
      TranslateTextResponse response = translate.projects().locations().translateText(request).setParent("projects/" + options.getProjectId() + "/locations/global").execute();
      return response.getTranslations();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}