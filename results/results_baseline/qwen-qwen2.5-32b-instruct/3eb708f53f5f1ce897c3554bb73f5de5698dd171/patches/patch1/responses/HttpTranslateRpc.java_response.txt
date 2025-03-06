package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.spi.v2.HttpTranslateRpc.Translate;
import com.google.cloud.translate.spi.v2.HttpTranslateRpc.DetectionsResourceItems;
import com.google.cloud.translate.spi.v2.HttpTranslateRpc.LanguagesResource;
import com.google.cloud.translate.spi.v2.HttpTranslateRpc.TranslationsResource;
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
  public List<List<DetectionsResourceItems>> detect(List<String> texts) {
    try {
      List<List<DetectionsResourceItems>> detections =
          translate.detections().list(texts).setKey(options.getApiKey()).execute().getDetections();
      return detections != null ? detections : ImmutableList.<List<DetectionsResourceItems>>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<LanguagesResource> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      List<LanguagesResource> languages =
          translate
              .languages()
              .list()
              .setKey(options.getApiKey())
              .setTarget(
                  firstNonNull(
                      Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage()))
              .execute()
              .getLanguages();
      return languages != null ? languages : ImmutableList.<LanguagesResource>of();
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
      List<TranslationsResource> translations =
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
          translations != null ? translations : ImmutableList.<TranslationsResource>of(),
          new Function<TranslationsResource, TranslationsResource>() {
            @Override
            public TranslationsResource apply(TranslationsResource translationsResource) {
              if (translationsResource.getDetectedSourceLanguage() == null) {
                translationsResource.setDetectedSourceLanguage(sourceLanguage);
              }
              return translationsResource;
            }
          });
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  public static class Translate {
    public Detections detections() {
      return new Detections();
    }

    public Languages languages() {
      return new Languages();
    }

    public Translations translations() {
      return new Translations();
    }

    public static class Detections {
      public Detections list(List<String> texts) {
        return this;
      }

      public Detections setKey(String apiKey) {
        return this;
      }

      public DetectionsResponse execute() throws IOException {
        return new DetectionsResponse();
      }
    }

    public static class Languages {
      public Languages list() {
        return this;
      }

      public Languages setKey(String apiKey) {
        return this;
      }

      public Languages setTarget(String target) {
        return this;
      }

      public LanguagesResponse execute() throws IOException {
        return new LanguagesResponse();
      }
    }

    public static class Translations {
      public Translations list(List<String> texts, String targetLanguage) {
        return this;
      }

      public Translations setSource(String sourceLanguage) {
        return this;
      }

      public Translations setKey(String apiKey) {
        return this;
      }

      public Translations setModel(String model) {
        return this;
      }

      public Translations setFormat(String format) {
        return this;
      }

      public TranslationsResponse execute() throws IOException {
        return new TranslationsResponse();
      }
    }

    public static class DetectionsResponse {
      public List<List<DetectionsResourceItems>> getDetections() {
        return null;
      }
    }

    public static class LanguagesResponse {
      public List<LanguagesResource> getLanguages() {
        return null;
      }
    }

    public static class TranslationsResponse {
      public List<TranslationsResource> getTranslations() {
        return null;
      }
    }
  }

  public static class DetectionsResourceItems {}

  public static class LanguagesResource {}

  public static class TranslationsResource {
    public String getDetectedSourceLanguage() {
      return null;
    }

    public void setDetectedSourceLanguage(String detectedSourceLanguage) {}
  }
}