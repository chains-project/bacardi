package com.google.cloud.translate.spi.v2;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.api.client.http.GenericUrl;
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translation;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Updated implementation of HttpTranslateRpc to work with the new dependency.
 */
public class HttpTranslateRpc implements TranslateRpc {

  private final TranslateOptions options;
  private final Translate translate;

  public HttpTranslateRpc(TranslateOptions options) {
    this.options = options;
    // Use the new client to obtain a Translate service instance.
    this.translate = options.getService();
  }

  private static TranslateException translate(Exception exception) {
    return new TranslateException(exception);
  }

  // Fixed buildTargetUrl to use options.getHost() instead of calling translate.getBaseUrl().
  private GenericUrl buildTargetUrl(String path) {
    GenericUrl genericUrl = new GenericUrl(options.getHost() + "v2/" + path);
    if (options.getApiKey() != null) {
      genericUrl.put("key", options.getApiKey());
    }
    return genericUrl;
  }

  @Override
  public List<List<Detection>> detect(List<String> texts) {
    try {
      // The new API returns a List<Detection> for the provided texts.
      List<Detection> detections = translate.detect(texts);
      // Wrap each Detection in a singleton list to match the original nested list signature.
      List<List<Detection>> wrappedDetections = new ArrayList<>(detections.size());
      for (Detection detection : detections) {
        wrappedDetections.add(ImmutableList.of(detection));
      }
      return wrappedDetections;
    } catch (Exception ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Language> listSupportedLanguages(Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      // Use the new API to list supported languages.
      List<Language> languages = translate.listSupportedLanguages(targetLanguage);
      return languages != null ? languages : ImmutableList.of();
    } catch (Exception ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<Translation> translate(List<String> texts, Map<Option, ?> optionMap) {
    try {
      String targetLanguage =
          firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
      final String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);

      // Build TranslateOption parameters based on the options map.
      List<Translate.TranslateOption> optionsList = new ArrayList<>();
      optionsList.add(Translate.TranslateOption.targetLanguage(targetLanguage));
      if (sourceLanguage != null) {
        optionsList.add(Translate.TranslateOption.sourceLanguage(sourceLanguage));
      }
      String model = Option.MODEL.getString(optionMap);
      if (model != null) {
        optionsList.add(Translate.TranslateOption.model(model));
      }
      String format = Option.FORMAT.getString(optionMap);
      if (format != null) {
        optionsList.add(Translate.TranslateOption.format(format));
      }
      Translate.TranslateOption[] optionsArray =
          optionsList.toArray(new Translate.TranslateOption[0]);

      // Call the new API method to perform translation.
      List<Translation> translations = translate.translate(texts, optionsArray);
      // The new Translation objects are immutable so we omit any setter adjustments.
      return translations != null ? translations : ImmutableList.of();
    } catch (Exception ex) {
      throw translate(ex);
    }
  }
}