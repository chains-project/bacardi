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
import com.google.api.services.translate.v3.Translate;
import com.google.api.services.translate.v3.Translate.Projects;
import com.google.api.services.translate.v3.Translate.Projects.Locations;
import com.google.api.services.translate.v3.model.DetectLanguageRequest;
import com.google.api.services.translate.v3.model.DetectLanguageResponse;
import com.google.api.services.translate.v3.model.Language;
import com.google.api.services.translate.v3.model.Location;
import com.google.api.services.translate.v3.model.SupportedLanguages;
import com.google.api.services.translate.v3.model.TranslateTextRequest;
import com.google.api.services.translate.v3.model.TranslateTextResponse;
import com.google.api.services.translate.v3.model.Translation;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.common.collect.ImmutableList;
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
  public List<List<com.google.api.services.translate.v3.model.DetectedLanguage>> detect(
      List<String> texts) {
    try {
      List<List<com.google.api.services.translate.v3.model.DetectedLanguage>> detections =
          new ArrayList<>();
      for (String text : texts) {
        DetectLanguageRequest request = new DetectLanguageRequest().setContent(text);
        Projects.DetectLanguage detect =
            translate
                .projects()
                .detectLanguage("projects/" + options.getProjectId())
                .setKey(options.getApiKey())
                .setRequest(request);
        DetectLanguageResponse response = detect.execute();
        detections.add(response.getLanguages());
      }
      return detections;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Language> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      String targetLanguageCode =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      String parent = "projects/" + options.getProjectId() + "/locations/global";

      Translate.Projects.Locations.GetSupportedLanguages list =
          translate.projects().locations().getSupportedLanguages(parent);
      list.setKey(options.getApiKey());
      if (targetLanguageCode != null) {
        list.setTarget(targetLanguageCode);
      }
      SupportedLanguages response = list.execute();
      return response.getLanguages();

    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<com.google.api.services.translate.v3.model.Translation> translate(
      List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
      String model = Option.MODEL.getString(optionMap);
      String format = Option.FORMAT.getString(optionMap);

      List<com.google.api.services.translate.v3.model.Translation> translations =
          new ArrayList<>();
      for (String text : texts) {
        TranslateTextRequest request = new TranslateTextRequest();
        request.setQ(text);
        request.setTargetLanguageCode(targetLanguage);
        if (sourceLanguage != null) {
          request.setSourceLanguageCode(sourceLanguage);
        }
        if (model != null) {
          request.setModel(model);
        }
        request.setFormat(format);

        Projects projects = translate.projects();
        Locations locations = projects.locations();
        Translate.Projects.Locations.TranslateText translateText =
            locations.translateText("projects/" + options.getProjectId() + "/locations/global", request);
        translateText.setKey(options.getApiKey());
        TranslateTextResponse response = translateText.execute();
        if (response.getTranslations() != null && !response.getTranslations().isEmpty()) {
          translations.add(response.getTranslations().get(0));
        }
      }
      return translations;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}