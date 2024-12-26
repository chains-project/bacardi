/*
 * Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
import com.google.cloud.translate.Language; // Updated import
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
    GenericUrl genericUrl = new GenericUrl("https://translation.googleapis.com/language/translate/v2/" + path); // Updated URL
    if (options.getApiKey() != null) {
      genericUrl.put("key", options.getApiKey());
    }
    return genericUrl;
  }

  @Override
  public List<List<Detection>> detect(List<String> texts) {
    try {
      List<List<Detection>> detections =
          translate.detections().list(texts).execute().getDetections(); // Updated call
      return detections != null ? detections : ImmutableList.<List<Detection>>of();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Language> listSupportedLanguages(Map<Option, ?> optionMap) { // Updated return type
    try {
      List<Language> languages =
          translate
              .languages()
              .list()
              .setTarget(
                  firstNonNull(
                      Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage()))
              .execute()
              .getLanguages();
      return languages != null ? languages : ImmutableList.<Language>of(); // Updated type
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
                  Translate.TranslateOption.format(Option.FORMAT.getString(optionMap)));
      return Lists.transform(
          translations != null ? translations : ImmutableList.<Translation>of(),
          new Function<Translation, Translation>() {
            @Override
            public Translation apply(Translation translation) {
              return translation; // Removed unnecessary check 
            }
          });
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}