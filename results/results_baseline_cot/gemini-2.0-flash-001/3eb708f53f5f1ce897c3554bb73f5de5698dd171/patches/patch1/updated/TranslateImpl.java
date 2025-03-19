package com.google.cloud.translate;

import static com.google.cloud.RetryHelper.runWithRetries;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

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
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

final class TranslateImpl extends BaseService<TranslateOptions> implements Translate {

  private final TranslateRpc translateRpc;

  private static final Function<List<com.google.cloud.translate.Detection>, Detection>
      DETECTION_FROM_PB_FUNCTION =
          new Function<List<com.google.cloud.translate.Detection>, Detection>() {
            @Override
            public Detection apply(List<com.google.cloud.translate.Detection> detectionPb) {
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
      List<List<com.google.cloud.translate.Detection>> detectionsPb =
          runWithRetries(
              new Callable<List<List<com.google.cloud.translate.Detection>>>() {
                @Override
                public List<List<com.google.cloud.translate.Detection>> call() {
                  return translateRpc.detect(texts);
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock());
      Iterator<List<com.google.cloud.translate.Detection>> detectionIterator = detectionsPb.iterator();
      Iterator<String> textIterator = texts.iterator();
      while (detectionIterator.hasNext() && textIterator.hasNext()) {
        List<com.google.cloud.translate.Detection> detectionPb = detectionIterator.next();
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