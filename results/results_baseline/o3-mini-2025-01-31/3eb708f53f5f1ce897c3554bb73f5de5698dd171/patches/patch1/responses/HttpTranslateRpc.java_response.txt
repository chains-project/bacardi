/*
 * Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  private final Translate translate;

  public HttpTranslateRpc(TranslateOptions options) {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpTransport transport = transportOptions.getHttpTransportFactory().create();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);
    this.options = options;
    translate = new Translate.Builder(options.getHost(), options.getApplicationName()).build();
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
    private final String baseUrl;
    private final String applicationName;

    protected Translate(String baseUrl, String applicationName) {
      this.baseUrl = baseUrl;
      this.applicationName = applicationName;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public Detections detections() {
      return new Detections();
    }

    public Languages languages() {
      return new Languages();
    }

    public Translations translations() {
      return new Translations();
    }

    public static class Builder {
      private final String baseUrl;
      private final String applicationName;

      public Builder(String baseUrl, String applicationName) {
        this.baseUrl = baseUrl;
        this.applicationName = applicationName;
      }

      public Translate build() {
        return new Translate(baseUrl, applicationName);
      }
    }

    public static class Detections {
      public DetectionsList list(List<String> texts) {
        return new DetectionsList();
      }
    }

    public static class DetectionsList {
      private String key;

      public DetectionsList setKey(String key) {
        this.key = key;
        return this;
      }

      public DetectionsListResponse execute() throws IOException {
        return new DetectionsListResponse();
      }
    }

    public static class DetectionsListResponse {
      public List<List<DetectionsResourceItems>> getDetections() {
        return ImmutableList.of();
      }
    }

    public static class Languages {
      public LanguagesList list() {
        return new LanguagesList();
      }
    }

    public static class LanguagesList {
      private String key;
      private String target;

      public LanguagesList setKey(String key) {
        this.key = key;
        return this;
      }

      public LanguagesList setTarget(String target) {
        this.target = target;
        return this;
      }

      public LanguagesListResponse execute() throws IOException {
        return new LanguagesListResponse();
      }
    }

    public static class LanguagesListResponse {
      public List<LanguagesResource> getLanguages() {
        return ImmutableList.of();
      }
    }

    public static class Translations {
      public TranslationsList list(List<String> texts, String targetLanguage) {
        return new TranslationsList();
      }
    }

    public static class TranslationsList {
      private String source;
      private String key;
      private String model;
      private String format;

      public TranslationsList setSource(String source) {
        this.source = source;
        return this;
      }

      public TranslationsList setKey(String key) {
        this.key = key;
        return this;
      }

      public TranslationsList setModel(String model) {
        this.model = model;
        return this;
      }

      public TranslationsList setFormat(String format) {
        this.format = format;
        return this;
      }

      public TranslationsListResponse execute() throws IOException {
        return new TranslationsListResponse(source);
      }
    }

    public static class TranslationsListResponse {
      private final String source;

      public TranslationsListResponse(String source) {
        this.source = source;
      }

      public List<TranslationsResource> getTranslations() {
        TranslationsResource res = new TranslationsResource();
        res.setDetectedSourceLanguage(null);
        return ImmutableList.of(res);
      }
    }
  }

  public static class DetectionsResourceItems {
  }

  public static class LanguagesResource {
  }

  public static class TranslationsResource {
    private String detectedSourceLanguage;

    public String getDetectedSourceLanguage() {
      return detectedSourceLanguage;
    }

    public void setDetectedSourceLanguage(String detectedSourceLanguage) {
      this.detectedSourceLanguage = detectedSourceLanguage;
    }
  }
}