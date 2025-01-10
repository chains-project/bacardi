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

import com.google.cloud.Service;
import com.google.cloud.translate.spi.v2.TranslateRpc;
import java.util.List;

/**
 * An interface for Google Translation. {@code Translate} and its {@code Option} classes can be used
 * concurrently without external synchronizations.
 *
 * @see <a href="https://cloud.google.com/translate/docs">Google Translation</a>
 */
public interface Translate extends Service<TranslateOptions> {

  /** Class for specifying supported language listing options. */
  class LanguageListOption extends Option {

    private static final long serialVersionUID = 1982978040516658597L;

    private LanguageListOption(TranslateRpc.Option rpcOption, String value) {
      super(rpcOption, value);
    }

    public static LanguageListOption targetLanguage(String targetLanguage) {
      return new LanguageListOption(TranslateRpc.Option.TARGET_LANGUAGE, targetLanguage);
    }
  }

  /** Class for specifying translate options. */
  class TranslateOption extends Option {

    private static final long serialVersionUID = 1347871763933507106L;

    private TranslateOption(TranslateRpc.Option rpcOption, String value) {
      super(rpcOption, value);
    }

    public static TranslateOption sourceLanguage(String sourceLanguage) {
      return new TranslateOption(TranslateRpc.Option.SOURCE_LANGUAGE, sourceLanguage);
    }

    public static TranslateOption targetLanguage(String targetLanguage) {
      return new TranslateOption(TranslateRpc.Option.TARGET_LANGUAGE, targetLanguage);
    }

    public static TranslateOption model(String model) {
      return new TranslateOption(TranslateRpc.Option.MODEL, model);
    }

    public static TranslateOption format(String format) {
      return new TranslateOption(TranslateRpc.Option.FORMAT, format);
    }
  }

  List<Language> listSupportedLanguages(LanguageListOption... options);

  List<Detection> detect(List<String> texts);

  List<Detection> detect(String... texts);

  Detection detect(String text);

  List<Translation> translate(List<String> texts, TranslateOption... options);

  Translation translate(String text, TranslateOption... options);
}