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
import com.google.cloud.translate.v3.TranslateClient;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.DetectLanguageRequest;
import com.google.cloud.translate.v3.DetectLanguageResponse;
import com.google.cloud.translate.v3.ListGlossariesRequest;
import com.google.cloud.translate.v3.ListGlossariesResponse;
import com.google.cloud.translate.v3.ListSupportedLanguagesRequest;
import com.google.cloud.translate.v3.ListSupportedLanguagesResponse;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HttpTranslateRpc implements TranslateRpc {

  private final TranslateOptions options;
  private final TranslateClient translate;

  public HttpTranslateRpc(TranslateOptions options) {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpTransport transport = transportOptions.getHttpTransportFactory().create();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);
    this.options = options;
    translate = TranslateClient.create(transport, new JacksonFactory(), initializer);
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
  public List<Translation> detect(List<String> texts) {
    try (TranslationServiceClient client = TranslationServiceClient.create()) {
      DetectLanguageRequest request = DetectLanguageRequest.newBuilder()
          .addAllContent(texts)
          .setParent(LocationName.of(options.getProjectId(), options.getLocation()).toString())
          .build();
      DetectLanguageResponse response = client.detectLanguage(request);
      return response.getLanguagesList();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<String> listSupportedLanguages(Map<Option, ?> optionMap) {
    try (TranslationServiceClient client = TranslationServiceClient.create()) {
      ListSupportedLanguagesRequest request = ListSupportedLanguagesRequest.newBuilder()
          .setParent(LocationName.of(options.getProjectId(), options.getLocation()).toString())
          .build();
      ListSupportedLanguagesResponse response = client.listSupportedLanguages(request);
      return response.getLanguagesList().stream().map(lang -> lang.getCode()).toList();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Translation> translate(List<String> texts, Map<Option, ?> optionMap) {
    try (TranslationServiceClient client = TranslationServiceClient.create()) {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
      TranslateTextRequest request = TranslateTextRequest.newBuilder()
          .addAllContents(texts)
          .setTargetLanguageCode(targetLanguage)
          .setSourceLanguageCode(sourceLanguage)
          .setModel(Option.MODEL.getString(optionMap))
          .setMimeType(Option.FORMAT.getString(optionMap))
          .build();
      TranslateTextResponse response = client.translateText(request);
      return response.getTranslationsList();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}
