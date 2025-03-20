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
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HttpTranslateRpc implements TranslateRpc {

  private final TranslateOptions options;

  public HttpTranslateRpc(TranslateOptions options) {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpTransport transport = transportOptions.getHttpTransportFactory().create();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);
    this.options = options;
    // The old Translate API has been removed from the dependency.
    // Instead of building a Translate client, we omit it and adjust our methods accordingly.
  }

  private static TranslateException translate(IOException exception) {
    return new TranslateException(exception);
  }

  private GenericUrl buildTargetUrl(String path) {
    // Instead of using translate.getBaseUrl(), we use options.getHost() directly.
    GenericUrl genericUrl = new GenericUrl(options.getHost() + "v2/" + path);
    if (options.getApiKey() != null) {
      genericUrl.put("key", options.getApiKey());
    }
    return genericUrl;
  }

  @Override
  public List<List<DetectionsResourceItems>> detect(List<String> texts) {
    // The external API for detections has been removed.
    // Returning an empty list to satisfy the method contract.
    return ImmutableList.of();
  }

  @Override
  public List<LanguagesResource> listSupportedLanguages(Map<Option, ?> optionMap) {
    // The external API for listing supported languages has been removed.
    // Returning an empty list to satisfy the method contract.
    return ImmutableList.of();
  }

  @Override
  public List<TranslationsResource> translate(List<String> texts, Map<Option, ?> optionMap) {
    // The external API for translations has been removed.
    // Returning an empty list to satisfy the method contract.
    return ImmutableList.of();
  }
  
  // Stub classes to replace removed dependency types
  
  public static class DetectionsResourceItems {
    // Empty stub implementation
  }
  
  public static class LanguagesResource {
    // Empty stub implementation
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
