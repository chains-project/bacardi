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

package com.google.cloud.translate;

import static com.google.cloud.RetryHelper.runWithRetries;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.services.translate.model.Detection; // Updated import
import com.google.api.services.translate.model.Language; // Updated import
import com.google.api.services.translate.model.Translation; // Updated import
import com.google.cloud.BaseService;
import com.google.cloud.RetryHelper.RetryHelperException;
import com.google.cloud.translate.spi.v2.TranslateRpc;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

final class TranslateImpl extends BaseService<TranslateOptions> implements Translate {

  private final TranslateRpc translateRpc;

  private static final Function<List<Detection>, Detection>
      DETECTION_FROM_PB_FUNCTION =
          new Function<List<Detection>, Detection>() {
            @Override
            public Detection apply(List<Detection> detectionPb) {
              return detectionPb.get(0);
            }
          };

  TranslateImpl(TranslateOptions options) {
    super(options);
    translateRpc = options.getTranslateRpcV2();
  }

  @Override
  public List<Language> listSupportedLanguages(final LanguageListOption... options) {
    try {
      return Lists.transform(
          runWithRetries(
              new Callable<List<Language>>() {
                @Override
                public List<Language> call() {
                  return translateRpc.listSupportedLanguages(optionMap(options));
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock()),
          Language.FROM_PB_FUNCTION);
    } catch (RetryHelperException e) {
      throw TranslateException.translateAndThrow(e);
    }
  }

  @Override
  public List<Detection> detect(final List<String> texts) {
    try {
      List<List<Detection>> detectionsPb =
          runWithRetries(
              new Callable<List<List<Detection>>>() {
                @Override
                public List<List<Detection>> call() {
                  return translateRpc.detect(texts);
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock());
      Iterator<List<Detection>> detectionIterator = detectionsPb.iterator();
      Iterator<String> textIterator = texts.iterator();
      while (detectionIterator.hasNext() && textIterator.hasNext()) {
        List<Detection> detectionPb = detectionIterator.next();
        String text = textIterator.next();
        checkState(
            detectionPb != null && !detectionPb.isEmpty(), "No detection found for text: %s", text);
        checkState(detectionPb.size() == 1, "Multiple detections found for text: %s", text);
      }
      return Lists.transform(detectionsPb, DETECTION_FROM_PB_FUNCTION);
    } catch (RetryHelperException e) {
      throw TranslateException.translateAndThrow(e);
    }
  }

  @Override
  public List<Detection> detect(String... texts) {
    return detect(Arrays.asList(texts));
  }

  @Override
  public Detection detect(String text) {
    return detect(Collections.singletonList(text)).get(0);
  }

  @Override
  public List<Translation> translate(final List<String> texts, final TranslateOption... options) {
    try {
      return Lists.transform(
          runWithRetries(
              new Callable<List<Translation>>() {
                @Override
                public List<Translation> call() {
                  return translateRpc.translate(texts, optionMap(options));
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock()),
          Translation.FROM_PB_FUNCTION);
    } catch (RetryHelperException e) {
      throw TranslateException.translateAndThrow(e);
    }
  }

  @Override
  public Translation translate(String text, TranslateOption... options) {
    return translate(Collections.singletonList(text), options).get(0);
  }

  private Map<TranslateRpc.Option, ?> optionMap(Option... options) {
    Map<TranslateRpc.Option, Object> optionMap = Maps.newEnumMap(TranslateRpc.Option.class);
    for (Option option : options) {
      Object prev = optionMap.put(option.getRpcOption(), option.getValue());
      checkArgument(prev == null, "Duplicate option %s", option);
    }
    return optionMap;
  }
}