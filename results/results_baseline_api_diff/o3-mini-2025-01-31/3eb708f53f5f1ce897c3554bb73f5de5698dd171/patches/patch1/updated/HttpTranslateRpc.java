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
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.api.services.translate.v3.Translate;
import com.google.api.services.translate.v3.model.DetectLanguageRequest;
import com.google.api.services.translate.v3.model.DetectLanguageResponse;
import com.google.api.services.translate.v3.model.DetectedLanguage;
import com.google.api.services.translate.v3.model.GetSupportedLanguagesResponse;
import com.google.api.services.translate.v3.model.SupportedLanguage;
import com.google.api.services.translate.v3.model.TranslateTextRequest;
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
    GenericUrl genericUrl = new GenericUrl(translate.getBaseUrl() + "v3/" + path);
    if (options.getApiKey() != null) {
      genericUrl.put("key", options.getApiKey());
    }
    return genericUrl;
  }

  @Override
  public List<List<DetectionsResourceItems>> detect(List<String> texts) {
    List<List<DetectionsResourceItems>> outerList = new ArrayList<>();
    String parent = "projects/" + options.getProjectId() + "/locations/global";
    try {
      for (String text : texts) {
        DetectLanguageRequest reqBody = new DetectLanguageRequest();
        reqBody.setContent(text);
        DetectLanguageResponse response =
            translate.projects().locations().detectLanguage(parent, reqBody)
                .setKey(options.getApiKey())
                .execute();
        List<DetectionsResourceItems> innerList = new ArrayList<>();
        if (response.getLanguages() != null) {
          for (DetectedLanguage detected : response.getLanguages()) {
            DetectionsResourceItems item = new DetectionsResourceItems();
            item.setLanguage(detected.getLanguageCode());
            item.setConfidence(detected.getConfidence());
            innerList.add(item);
          }
        }
        outerList.add(innerList);
      }
      return outerList;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<LanguagesResource> listSupportedLanguages(Map<Option, ?> optionMap) {
    String targetLanguage = firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
    String parent = "projects/" + options.getProjectId() + "/locations/global";
    try {
      GetSupportedLanguagesResponse response =
          translate.projects().locations().getSupportedLanguages(parent)
              .setKey(options.getApiKey())
              .setTargetLanguage(targetLanguage)
              .execute();
      List<LanguagesResource> languagesList = new ArrayList<>();
      if (response.getLanguages() != null) {
        for (SupportedLanguage lang : response.getLanguages()) {
          LanguagesResource lr = new LanguagesResource();
          lr.setLanguage(lang.getLanguageCode());
          lr.setName(lang.getDisplayName());
          languagesList.add(lr);
        }
      }
      return languagesList;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public List<TranslationsResource> translate(List<String> texts, Map<Option, ?> optionMap) {
    String targetLanguage = firstNonNull(Option.TARGET_LANGUAGE.getString(optionMap), options.getTargetLanguage());
    final String sourceLanguage = Option.SOURCE_LANGUAGE.getString(optionMap);
    String parent = "projects/" + options.getProjectId() + "/locations/global";
    TranslateTextRequest requestBody = new TranslateTextRequest();
    requestBody.setContents(texts);
    requestBody.setTargetLanguageCode(targetLanguage);
    if (sourceLanguage != null) {
      requestBody.setSourceLanguageCode(sourceLanguage);
    }
    String model = Option.MODEL.getString(optionMap);
    if (model != null) {
      requestBody.setModel(model);
    }
    String format = Option.FORMAT.getString(optionMap);
    if (format != null) {
      requestBody.setMimeType(format);
    }
    try {
      TranslateTextResponse response =
          translate.projects().locations().translateText(parent, requestBody)
              .setKey(options.getApiKey())
              .execute();
      List<TranslationsResource> translations = new ArrayList<>();
      if (response.getTranslations() != null) {
        for (Translation t : response.getTranslations()) {
          TranslationsResource tr = new TranslationsResource();
          tr.setTranslatedText(t.getTranslatedText());
          String detected = t.getDetectedLanguageCode();
          if (detected == null) {
            tr.setDetectedSourceLanguage(sourceLanguage);
          } else {
            tr.setDetectedSourceLanguage(detected);
          }
          translations.add(tr);
        }
      }
      return translations;
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  public static class DetectionsResourceItems {
    private String language;
    private Float confidence;

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public Float getConfidence() {
      return confidence;
    }

    public void setConfidence(Float confidence) {
      this.confidence = confidence;
    }
  }

  public static class LanguagesResource {
    private String language;
    private String name;

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class TranslationsResource {
    private String translatedText;
    private String detectedSourceLanguage;

    public String getTranslatedText() {
      return translatedText;
    }

    public void setTranslatedText(String translatedText) {
      this.translatedText = translatedText;
    }

    public String getDetectedSourceLanguage() {
      return detectedSourceLanguage;
    }

    public void setDetectedSourceLanguage(String detectedSourceLanguage) {
      this.detectedSourceLanguage = detectedSourceLanguage;
    }
  }
}