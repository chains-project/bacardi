package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.spi.v3.TranslateRpc;
import com.google.cloud.translate.v3.TranslateClient;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.ListGlossariesRequest;
import com.google.cloud.translate.v3.ListGlossariesResponse;
import com.google.cloud.translate.v3.Glossary;
import com.google.cloud.translate.v3.ListSupportedLanguagesRequest;
import com.google.cloud.translate.v3.ListSupportedLanguagesResponse;
import com.google.cloud.translate.v3.SupportedLanguage;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectLanguageResponse;
import com.google.cloud.translate.v3.DetectedLanguage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HttpTranslateRpc implements TranslateRpc {

  private final TranslateOptions options;
  private final TranslateClient translate;

  public HttpTranslateRpc(TranslateOptions options) {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpTransport transport = transportOptions.getHttpTransportFactory().create();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);
    this.options = options;
    translate = TranslateClient.create(transport, new JacksonFactory(), initializer);
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
  public List<List<DetectedLanguage>> detect(List<String> texts) {
    try {
      DetectLanguageRequest request = DetectLanguageRequest.newBuilder()
          .setParent(LocationName.of(options.getProjectId(), options.getLocation()).toString())
          .addAllTexts(texts)
          .build();
      DetectLanguageResponse response = translate.detectLanguage(request);
      return response.getLanguagesList();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<SupportedLanguage> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      ListSupportedLanguagesRequest request = ListSupportedLanguagesRequest.newBuilder()
          .setParent(LocationName.of(options.getProjectId(), options.getLocation()).toString())
          .build();
      ListSupportedLanguagesResponse response = translate.listSupportedLanguages(request);
      return response.getLanguagesList();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Translation> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage = firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      TranslateTextRequest request = TranslateTextRequest.newBuilder()
          .setParent(LocationName.of(options.getProjectId(), options.getLocation()).toString())
          .addAllText(texts)
          .setTargetLanguageCode(targetLanguage)
          .build();
      TranslateTextResponse response = translate.translateText(request);
      return response.getTranslationsList();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}