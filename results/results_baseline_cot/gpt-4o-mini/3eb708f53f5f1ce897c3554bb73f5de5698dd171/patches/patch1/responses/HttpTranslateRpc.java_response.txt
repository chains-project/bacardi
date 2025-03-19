package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.Translation;
import com.google.cloud.http.HttpTransportOptions;
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
  public List<List<Detection>> detect(List<String> texts) {
    try {
      List<List<Detection>> detections =
          translate.detections().list(texts).setKey(options.getApiKey()).execute().getDetections();
      return detections != null ? detections : ImmutableList.<List<Detection>>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Language> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      List<Language> languages =
          translate
              .languages()
              .list()
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
  public List<Translation> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      final String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
      List<Translation> translations =
          translate
              .translations()
              .list(texts, targetLanguage)
              .setSource(sourceLanguage)
              .setKey(options.getApiKey())
              .setModel(Option.MODEL.getString(optionMap))
              .setFormat(Option.FORMAT.getString(optionMap))
              .execute()
              .getTranslations();
      return Lists.transform(
          translations != null ? translations : ImmutableList.<Translation>of(),
          new Function<Translation, Translation>() {
            @Override
            public Translation apply(Translation translation) {
              if (translation.getDetectedSourceLanguage() == null) {
                translation.setDetectedSourceLanguage(sourceLanguage);
              }
              return translation;
            }
          });
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}