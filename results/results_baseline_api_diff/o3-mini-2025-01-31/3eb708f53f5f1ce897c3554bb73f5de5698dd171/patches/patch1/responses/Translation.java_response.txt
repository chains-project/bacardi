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

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Translation implements Serializable {

  private static final long serialVersionUID = 2556017420486245581L;
  static final Function<com.google.api.services.translate.v3.model.Translation, Translation>
      FROM_PB_FUNCTION =
          new Function<com.google.api.services.translate.v3.model.Translation, Translation>() {
            @Override
            public Translation apply(
                com.google.api.services.translate.v3.model.Translation translationPb) {
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

  static Translation fromPb(
      com.google.api.services.translate.v3.model.Translation translationPb) {
    return new Translation(
        translationPb.getTranslatedText(),
        null,
        translationPb.getModel());
  }
}