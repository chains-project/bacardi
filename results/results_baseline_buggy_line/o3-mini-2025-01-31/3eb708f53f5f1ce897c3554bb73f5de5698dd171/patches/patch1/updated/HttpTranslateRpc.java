package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.v2.Translate;
import com.google.api.services.translate.v2.model.Detection;
import com.google.api.services.translate.v2.model.Language;
import com.google.api.services.translate.v2.model.Translation;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.base.Function;
import java.io.IOException;
import java.util.ArrayList;
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
  public List<List<DetectionsResourceItems>> detect(List<String> texts) {
    try {
      List<List<Detection>> detectionLists =
          translate.detections().list(texts).setKey(options.getApiKey()).execute().getDetections();
      if (detectionLists == null) {
        return ImmutableList.of();
      }
      List<List<DetectionsResourceItems>> result = new ArrayList<>();
      for (List<Detection> innerList : detectionLists) {
        List<DetectionsResourceItems> transformedInner = new ArrayList<>();
        for (Detection d : innerList) {
          transformedInner.add(new DetectionsResourceItems(d));
        }
        result.add(transformedInner);
      }
      return result;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<LanguagesResource> listSupportedLanguages(Map<Option, ?> optionMap) {
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
      if (languages == null) {
        return ImmutableList.of();
      }
      List<LanguagesResource> result = new ArrayList<>();
      for (Language language : languages) {
        result.add(new LanguagesResource(language));
      }
      return result;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<TranslationsResource> translate(List<String> texts, Map<Option, ?> optionMap) {
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
      List<TranslationsResource> transformedTranslations = new ArrayList<>();
      if (translations != null) {
        for (Translation t : translations) {
          TranslationsResource tr = new TranslationsResource(t);
          if (tr.getDetectedSourceLanguage() == null) {
            tr.setDetectedSourceLanguage(sourceLanguage);
          }
          transformedTranslations.add(tr);
        }
      }
      return transformedTranslations;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  public static class DetectionsResourceItems extends Detection {
    public DetectionsResourceItems() {
      super();
    }

    public DetectionsResourceItems(Detection detection) {
      this.setConfidence(detection.getConfidence());
      this.setIsReliable(detection.getIsReliable());
      this.setLanguage(detection.getLanguage());
    }
  }

  public static class LanguagesResource extends Language {
    public LanguagesResource() {
      super();
    }

    public LanguagesResource(Language language) {
      this.setLanguage(language.getLanguage());
      this.setName(language.getName());
    }
  }

  public static class TranslationsResource extends Translation {
    public TranslationsResource() {
      super();
    }

    public TranslationsResource(Translation translation) {
      this.setTranslatedText(translation.getTranslatedText());
      this.setDetectedSourceLanguage(translation.getDetectedSourceLanguage());
    }
  }
}