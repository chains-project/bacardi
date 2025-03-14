package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.v3.Translate;
import com.google.api.services.translate.v3.model.DetectLanguageResponse;
import com.google.api.services.translate.v3.model.Language;
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
    GenericUrl genericUrl = new GenericUrl(translate.getRootUrl() + "v3/" + path);
    if (options.getApiKey() != null) {
      genericUrl.put("key", options.getApiKey());
    }
    return genericUrl;
  }

  @Override
  public List<List<DetectLanguageResponse>> detect(List<String> texts) {
    try {
      List<List<DetectLanguageResponse>> detections =
          translate.projects().locations().detectLanguage("projects/" + options.getProjectId())
              .setKey(options.getApiKey())
              .execute().getLanguages();
      return detections != null ? detections : ImmutableList.<List<DetectLanguageResponse>>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Language> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      List<Language> languages =
          translate.projects().locations().getSupportedLanguages("projects/" + options.getProjectId())
              .setKey(options.getApiKey())
              .setTarget(
                  firstNonNull(
                      Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage()))
              .execute()
              .getLanguages();
      return languages != null ? languages : ImmutableList.<Language>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<TranslateTextResponse> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      final String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
      List<TranslateTextResponse> translations =
          translate.projects().locations().translateText("projects/" + options.getProjectId(), texts, targetLanguage)
              .setSource(sourceLanguage)
              .setKey(options.getApiKey())
              .setModel(Option.MODEL.getString(optionMap))
              .setFormat(Option.FORMAT.getString(optionMap))
              .execute()
              .getTranslations();
      return Lists.transform(
          translations != null ? translations : ImmutableList.<TranslateTextResponse>of(),
          new Function<TranslateTextResponse, TranslateTextResponse>() {
            @Override
            public TranslateTextResponse apply(TranslateTextResponse translateTextResponse) {
              if (translateTextResponse.getDetectedSourceLanguage() == null) {
                translateTextResponse.setDetectedSourceLanguage(sourceLanguage);
              }
              return translateTextResponse;
            }
          });
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}