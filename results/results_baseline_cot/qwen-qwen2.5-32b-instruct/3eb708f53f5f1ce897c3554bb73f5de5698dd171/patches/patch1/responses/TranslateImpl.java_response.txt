package com.google.cloud.translate;

import static com.google.cloud.RetryHelper.runWithRetries;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.cloud.BaseService;
import com.google.cloud.RetryHelper.RetryHelperException;
import com.google.cloud.translate.model.DetectionResource;
import com.google.cloud.translate.model.LanguageResource;
import com.google.cloud.translate.model.TranslationResource;
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

  private static final Function<List<DetectionResource>, Detection>
      DETECTION_FROM_PB_FUNCTION =
          new Function<List<DetectionResource>, Detection>() {
            @Override
            public Detection apply(List<DetectionResource> detectionPb) {
              return Detection.fromPb(detectionPb.get(0));
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
              new Callable<List<LanguageResource>>() {
                @Override
                public List<LanguageResource> call() {
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
      List<List<DetectionResource>> detectionsPb =
          runWithRetries(
              new Callable<List<List<DetectionResource>>>() {
                @Override
                public List<List<DetectionResource>> call() {
                  return translateRpc.detect(texts);
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock());
      Iterator<List<DetectionResource>> detectionIterator = detectionsPb.iterator();
      Iterator<String> textIterator = texts.iterator();
      while (detectionIterator.hasNext() && textIterator.hasNext()) {
        List<DetectionResource> detectionPb = detectionIterator.next();
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
  public Detection detect(String text) {
    return detect(Collections.singletonList(text)).get(0);
  }

  @Override
  public List<Translation> translate(final List<String> texts, final TranslateOption... options) {
    try {
      return Lists.transform(
          runWithRetries(
              new Callable<List<TranslationResource>>() {
                @Override
                public List<TranslationResource> call() {
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

  private Map<TranslateRpc.Option, ?> optionMap(Option... options) {
    Map<TranslateRpc.Option, Object> optionMap = Maps.newEnumMap(TranslateRpc.Option.class);
    for (Option option : options) {
      Object prev = optionMap.put(option.getRpcOption(), option.getValue());
      checkArgument(prev == null, "Duplicate option %s", option);
    }
    return optionMap;
  }
}