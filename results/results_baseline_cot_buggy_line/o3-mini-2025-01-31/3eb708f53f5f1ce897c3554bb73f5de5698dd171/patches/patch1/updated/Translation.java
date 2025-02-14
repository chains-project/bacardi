package com.google.cloud.translate;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Information about a translation. Objects of this class contain the translated text and the source
 * language's code. The source language's code can be optionally specified by the user or
 * automatically detected by the Google Translation service.
 *
 * @see <a href="https://cloud.google.com/translate/v2/translating-text-with-rest">Translating
 *     Text</a>
 */
public class Translation implements Serializable {

  private static final long serialVersionUID = 2556017420486245581L;

  // Using our own inner class TranslationsResource (stub) as a replacement for the removed dependency.
  static final Function<TranslationsResource, Translation> FROM_PB_FUNCTION =
      new Function<TranslationsResource, Translation>() {
        @Override
        public Translation apply(TranslationsResource translationPb) {
          return Translation.fromPb(translationPb);
        }
      };

  private final String translatedText;
  private final String sourceLanguage;
  private final String model;

  private Translation(String translatedText, String sourceLanguage, String model) {
    this.translatedText = translatedText;
    this.sourceLanguage = sourceLanguage;
    this.model = model;
  }

  /** Returns the translated text. */
  public String getTranslatedText() {
    return translatedText;
  }

  /**
   * Returns the language code of the source text. If no source language was provided this value is
   * the source language as detected by the Google Translation service.
   */
  public String getSourceLanguage() {
    return sourceLanguage;
  }

  /**
   * Returns the translation model used to translate the text. This value is only available if a
   * result from {@link Translate.TranslateOption#model(String)} was passed to {@link
   * Translate#translate(List, Translate.TranslateOption...)}.
   *
   * <p>Please note that you must be whitelisted to use the {@link
   * Translate.TranslateOption#model(String)} option, otherwise translation will fail.
   */
  public String getModel() {
    return model;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("translatedText", translatedText)
        .add("sourceLanguage", sourceLanguage)
        .toString();
  }

  @Override
  public final int hashCode() {
    return Objects.hash(translatedText, sourceLanguage);
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !obj.getClass().equals(Translation.class)) {
      return false;
    }
    Translation other = (Translation) obj;
    return Objects.equals(translatedText, other.translatedText)
        && Objects.equals(sourceLanguage, other.sourceLanguage);
  }

  static Translation fromPb(TranslationsResource translationPb) {
    return new Translation(
        translationPb.getTranslatedText(),
        translationPb.getDetectedSourceLanguage(),
        translationPb.getModel());
  }

  /**
   * A stub replacement for the removed
   * com.google.api.services.translate.model.TranslationsResource class.
   * This minimal implementation provides the methods needed by Translation.fromPb.
   */
  public static class TranslationsResource implements Serializable {

    private static final long serialVersionUID = 1L;
    private String translatedText;
    private String detectedSourceLanguage;
    private String model;

    public TranslationsResource() {
    }

    public TranslationsResource(String translatedText, String detectedSourceLanguage, String model) {
      this.translatedText = translatedText;
      this.detectedSourceLanguage = detectedSourceLanguage;
      this.model = model;
    }

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

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }
  }
}