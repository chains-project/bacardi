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
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.cloud.translate.Translations;
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Languages;
import com.google.cloud.translate.Translate;
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
        TranslateOptions.newBuilder()
            .setHttpTransport(transport)
            .setJsonFactory(new JacksonFactory())
            .setCredentials(initializer)
            .setHost(options.getHost())
            .setApplicationName(options.getApplicationName())
            .build()
            .getService();
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
  public List<Languages> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      List<Languages> languages =
          translate
              .languages()
              .list()
              .setKey(options.getApiKey())
              .setTarget(
                  firstNonNull(
                      Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage()))
              .execute()
              .getLanguages();
      return languages != null ? languages : ImmutableList.<Languages>of();
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
              .translate(texts, Translate.TranslateOption.targetLanguage(targetLanguage),
                  Translate.TranslateOption.sourceLanguage(sourceLanguage),
                  Translate.TranslateOption.apiKey(options.getApiKey()),
                  Translate.TranslateOption.model(Option.MODEL.getString(optionMap)),
                  Translate.TranslateOption.format(Option.FORMAT.getString(optionMap)));
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