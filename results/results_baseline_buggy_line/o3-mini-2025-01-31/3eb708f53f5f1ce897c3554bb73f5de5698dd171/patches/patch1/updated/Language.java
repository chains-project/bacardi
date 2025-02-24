package com.google.cloud.translate;

import com.google.api.services.translate.model.LanguageList; // Updated import
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Objects;

/**
 * Information about a language supported by Google Translation. Objects of this class contain the
 * language's code and the language name.
 *
 * @see <a href="https://cloud.google.com/translate/v2/discovering-supported-languages-with-rest">
 *     Discovering Supported Languages</a>
 * @see <a href="https://cloud.google.com/translate/docs/languages">Supported Languages</a>
 */
public class Language implements Serializable {

  private static final long serialVersionUID = 5205240279371907020L;
  static final Function<LanguageList.Language, Language> FROM_PB_FUNCTION = // Updated type
      new Function<LanguageList.Language, Language>() { // Updated type
        public Language apply(LanguageList.Language languagePb) { // Updated type
          return Language.fromPb(languagePb); // Updated type
        }
      };

  private final String code;
  private final String name;

  private Language(String code, String name) {
    this.code = code;
    this.name = name;
  }

  /** Returns the code of the language. */
  public String getCode() {
    return code;
  }

  /** Returns the name of the language. */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("code", code).add("name", name).toString();
  }

  @Override
  public final int hashCode() {
    return Objects.hash(code, name);
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !obj.getClass().equals(Language.class)) {
      return false;
    }
    Language other = (Language) obj;
    return Objects.equals(code, other.code) && Objects.equals(name, other.name);
  }

  static Language fromPb(LanguageList.Language languagePb) { // Updated type
    return new Language(languagePb.getLanguage(), languagePb.getName());
  }
}