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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.base.Function;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.api.services.translate.v3.Translate;
import com.google.api.services.translate.v3.model.DetectLanguageRequest;
import com.google.api.services.translate.v3.model.DetectLanguageResponse;
import com.google.api.services.translate.v3.model.DetectedLanguage;
import com.google.api.services.translate.v3.model.GetSupportedLanguagesResponse;
import com.google.api.services.translate.v3.model.Language;
import com.google.api.services.translate.v3.model.TranslateTextRequest;
import com.google.api.services.translate.v3.model.TranslateTextResponse;
import com.google.api.services.translate.v3.model.Translation;

public class HttpTranslateRpc implements TranslateRpc {

  private final Translate translate;
  private final TranslateOptions options;

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
  public List<List<DetectionsResourceItems>> detect(List<String> texts) {
    try {
      String parent = "projects/" + options.getProjectId() + "/locations/global";
      List<List<DetectionsResourceItems>> allDetections = new ArrayList<>();
      for (String text : texts) {
        DetectLanguageRequest request = new DetectLanguageRequest();
        request.setContent(text);
        DetectLanguageResponse response =
            translate.projects().locations().detectLanguage(parent, request)
                .setKey(options.getApiKey())
                .execute();
        List<DetectionsResourceItems> detectionList = new ArrayList<>();
        if (response.getLanguages() != null) {
          for (DetectedLanguage dl : response.getLanguages()) {
            DetectionsResourceItems item = new DetectionsResourceItems();
            item.setLanguage(dl.getLanguageCode());
            item.setConfidence(dl.getConfidence() != null ? dl.getConfidence().floatValue() : 0.0f);
            detectionList.add(item);
          }
        }
        allDetections.add(detectionList);
      }
      return allDetections != null ? allDetections : ImmutableList.<List<DetectionsResourceItems>>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<LanguagesResource> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      String parent = "projects/" + options.getProjectId() + "/locations/global";
      GetSupportedLanguagesResponse response =
          translate.projects().locations().getSupportedLanguages(parent)
              .setKey(options.getApiKey())
              .setTargetLanguageCode(targetLanguage)
              .execute();
      List<LanguagesResource> languages = new ArrayList<>();
      if (response.getLanguages() != null) {
        for (Language lang : response.getLanguages()) {
          LanguagesResource lr = new LanguagesResource();
          lr.setLanguage(lang.getLanguageCode());
          lr.setName(lang.getDisplayName());
          languages.add(lr);
        }
      }
      return languages != null ? languages : ImmutableList.<LanguagesResource>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<TranslationsResource> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      final String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      final String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
      final String model = Option.MODEL.getString(optionMap);
      final String format = Option.FORMAT.getString(optionMap);
      String parent = "projects/" + options.getProjectId() + "/locations/global";
      TranslateTextRequest request = new TranslateTextRequest();
      request.setContents(texts);
      request.setTargetLanguageCode(targetLanguage);
      if (sourceLanguage != null) {
        request.setSourceLanguageCode(sourceLanguage);
      }
      if (format != null) {
        request.setMimeType(format);
      }
      if (model != null) {
        request.setModel(model);
      }
      TranslateTextResponse response =
          translate.projects().locations().translateText(parent, request)
              .setKey(options.getApiKey())
              .execute();
      List<TranslationsResource> translations = new ArrayList<>();
      if (response.getTranslations() != null) {
        for (Translation t : response.getTranslations()) {
          TranslationsResource tr = new TranslationsResource();
          tr.setTranslatedText(t.getTranslatedText());
          if (t.getDetectedLanguageCode() == null) {
            tr.setDetectedSourceLanguage(sourceLanguage);
          } else {
            tr.setDetectedSourceLanguage(t.getDetectedLanguageCode());
          }
          translations.add(tr);
        }
      }
      return translations != null ? translations : ImmutableList.<TranslationsResource>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  public static class DetectionsResourceItems {
    private String language;
    private float confidence;
    private String detectedSourceLanguage;

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public float getConfidence() {
      return confidence;
    }

    public void setConfidence(float confidence) {
      this.confidence = confidence;
    }

    public String getDetectedSourceLanguage() {
      return detectedSourceLanguage;
    }

    public void setDetectedSourceLanguage(String detectedSourceLanguage) {
      this.detectedSourceLanguage = detectedSourceLanguage;
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