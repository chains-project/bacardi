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
import com.google.cloud.translate.Translation;
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Language;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.google.api.services.translate.Translate;

public class HttpTranslateRpc implements TranslateRpc {

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
      Translate.Detections.List request = translate.detections().list(texts);
      if (options.getApiKey() != null) {
        request.setKey(options.getApiKey());
      }
      List<List<Detection>> detections = request.execute().getDetections();
      return detections != null ? detections : ImmutableList.<List<Detection>>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Language> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      Translate.Languages.List request = translate.languages().list();
      if (options.getApiKey() != null) {
        request.setKey(options.getApiKey());
      }
      request.setTarget(
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage()));
      List<Language> languages = request.execute().getLanguages();
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
      Translate.Translations.List request = translate.translations().list(texts, targetLanguage);
      if (sourceLanguage != null) {
        request.setSource(sourceLanguage);
      }
      if (options.getApiKey() != null) {
        request.setKey(options.getApiKey());
      }
      String model = Option.MODEL.getString(optionMap);
      if (model != null) {
        request.setModel(model);
      }
      String format = Option.FORMAT.getString(optionMap);
      if (format != null) {
        request.setFormat(format);
      }
      List<Translation> translations = request.execute().getTranslations();
      return Lists.transform(
          translations != null ? translations : ImmutableList.<Translation>of(),
          new Function<Translation, Translation>() {
            @Override
            public Translation apply(Translation translationsResource) {
              if (translationsResource.getSourceLanguage() == null && sourceLanguage != null) {
                translationsResource.setSourceLanguage(sourceLanguage);
              }
              return translationsResource;
            }
          });
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}