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
import com.google.api.services.translate.v3.Translate;
import com.google.api.services.translate.v3.model.DetectLanguageRequest;
import com.google.api.services.translate.v3.model.DetectLanguageResponse;
import com.google.api.services.translate.v3.model.SupportedLanguages;
import com.google.api.services.translate.v3.model.SupportedLanguages.Language;
import com.google.api.services.translate.v3.model.TranslateTextRequest;
import com.google.api.services.translate.v3.model.TranslateTextResponse;
import com.google.api.services.translate.v3.model.TranslateTextResponse.Translation;
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
    GenericUrl genericUrl = new GenericUrl(translate.getBaseUrl() + "v3/" + path);
    if (options.getApiKey() != null) {
      genericUrl.put("key", options.getApiKey());
    }
    return genericUrl;
  }

  @Override
  public List<List<DetectionsResourceItems>> detect(List<String> texts) {
    try {
      List<List<DetectionsResourceItems>> allDetections = new ArrayList<>();
      String parent = "projects/_/locations/global";
      for (String text : texts) {
        DetectLanguageRequest request = new DetectLanguageRequest();
        request.setContent(text);
        DetectLanguageResponse response =
            translate
                .projects()
                .locations()
                .detectLanguage(parent, request)
                .setKey(options.getApiKey())
                .execute();
        List<DetectionsResourceItems> detectionList = new ArrayList<>();
        if (response != null && response.getDetections() != null) {
          for (Object obj : response.getDetections()) {
            com.google.api.services.translate.v3.model.DetectLanguageResponse.Detection detection =
                (com.google.api.services.translate.v3.model.DetectLanguageResponse.Detection) obj;
            DetectionsResourceItems item = new DetectionsResourceItems();
            item.setLanguage(detection.getLanguage());
            item.setConfidence(detection.getConfidence());
            detectionList.add(item);
          }
        }
        allDetections.add(detectionList);
      }
      return allDetections;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<LanguagesResource> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      String parent = "projects/_/locations/global";
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      SupportedLanguages suppLang =
          translate
              .projects()
              .locations()
              .getSupportedLanguages(parent)
              .setKey(options.getApiKey())
              .setTargetLanguageCode(targetLanguage)
              .execute();
      List<LanguagesResource> languages = new ArrayList<>();
      if (suppLang != null && suppLang.getLanguages() != null) {
        for (Language lang : suppLang.getLanguages()) {
          LanguagesResource res = new LanguagesResource();
          res.setLanguage(lang.getLanguageCode());
          res.setName(lang.getDisplayName());
          languages.add(res);
        }
      }
      return languages.isEmpty() ? ImmutableList.<LanguagesResource>of() : languages;
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
      TranslateTextRequest req = new TranslateTextRequest();
      req.setContents(texts);
      req.setTargetLanguageCode(targetLanguage);
      if (sourceLanguage != null) {
        req.setSourceLanguageCode(sourceLanguage);
      }
      String model = Option.MODEL.getString(optionMap);
      if (model != null) {
        req.setModel(model);
      }
      String format = Option.FORMAT.getString(optionMap);
      if (format != null) {
        req.setMimeType("html".equalsIgnoreCase(format) ? "text/html" : "text/plain");
      }
      String parent = "projects/_/locations/global";
      TranslateTextResponse response =
          translate
              .projects()
              .locations()
              .translateText(parent, req)
              .setKey(options.getApiKey())
              .execute();
      List<TranslationsResource> results = new ArrayList<>();
      if (response != null && response.getTranslations() != null) {
        for (Translation t : response.getTranslations()) {
          TranslationsResource res = new TranslationsResource();
          res.setTranslatedText(t.getTranslatedText());
          res.setDetectedSourceLanguage(t.getDetectedLanguageCode());
          results.add(res);
        }
      }
      return Lists.transform(
          results,
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

  public static class DetectionsResourceItems {
    private String language;
    private Float confidence;

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public Float getConfidence() {
      return confidence;
    }

    public void setConfidence(Float confidence) {
      this.confidence = confidence;
    }
  }

  public static class LanguagesResource {
    private String language;
    private String name;

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class TranslationsResource {
    private String translatedText;
    private String detectedSourceLanguage;

    public String getTranslatedText() {
      return translatedText;
    }

    public void setTranslatedText(String translatedText) {
      this.translatedText = translatedText;
    }

    public String getDetectedSourceLanguage() {
      return detectedSourceLanguage;
    }

    public void setDetectedSourceLanguage(String detectedSourceLanguage) {
      this.detectedSourceLanguage = detectedSourceLanguage;
    }
  }
}
