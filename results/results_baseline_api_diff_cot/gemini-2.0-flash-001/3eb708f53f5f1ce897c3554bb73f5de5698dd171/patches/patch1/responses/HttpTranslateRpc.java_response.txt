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
 * is distributed on an "AS IS" BASIS,
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
import com.google.api.services.translate.v3.Translate;
import com.google.api.services.translate.v3.model.DetectLanguageResponse;
import com.google.api.services.translate.v3.model.Language;
import com.google.api.services.translate.v3.model.TranslateTextResponse;
import com.google.api.services.translate.v3.model.Translation;

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
  public List<List<com.google.api.services.translate.v3.model.Detection>> detect(List<String> texts) {
    try {
      ImmutableList.Builder<List<com.google.api.services.translate.v3.model.Detection>> detectionsResult = ImmutableList.builder();
      for (String text : texts) {
        Translate.Projects.DetectLanguage detectLanguage = translate.projects().detectLanguage("projects/" + options.getProjectId());
        detectLanguage.setQ(ImmutableList.of(text));
        detectLanguage.setKey(options.getApiKey());
        DetectLanguageResponse response = detectLanguage.execute();
        detectionsResult.add(response.getDetections());
      }
      return detectionsResult.build();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Language> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(
              Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      Translate.Projects.Locations.GetSupportedLanguages list = translate.projects().locations().getSupportedLanguages("projects/" + options.getProjectId() + "/locations/" + options.getLocation());
      list.setTarget(targetLanguage);
      list.setKey(options.getApiKey());
      List<Language> languages = list.execute().getLanguages();
      return languages != null ? languages : ImmutableList.<Language>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<com.google.api.services.translate.v3.model.Translation> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      final String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
      ImmutableList.Builder<com.google.api.services.translate.v3.model.Translation> translationsResult = ImmutableList.builder();

      for (String text : texts) {
        Translate.Projects.Locations.TranslateText translateText = translate.projects().locations().translateText("projects/" + options.getProjectId() + "/locations/" + options.getLocation(), new com.google.api.services.translate.v3.model.TranslateTextRequest().setContents(ImmutableList.of(text)).setTargetLanguageCode(targetLanguage).setSourceLanguageCode(sourceLanguage).setMimeType("text/plain"));
        translateText.setKey(options.getApiKey());
        TranslateTextResponse response = translateText.execute();
        translationsResult.addAll(response.getTranslations());
      }

      return Lists.transform(
          translationsResult.build(),
          new Function<com.google.api.services.translate.v3.model.Translation, com.google.api.services.translate.v3.model.Translation>() {
            @Override
            public com.google.api.services.translate.v3.model.Translation apply(com.google.api.services.translate.v3.model.Translation translationsResource) {
              return translationsResource;
            }
          });
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}