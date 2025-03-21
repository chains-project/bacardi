final class TranslateImpl extends BaseService<TranslateOptions> implements Translate {

  private final TranslateRpc translateRpc;

  private static final Function<List<Object>, Detection>
      DETECTION_FROM_PB_FUNCTION =
          new Function<List<Object>, Detection>() {
            @Override
            public Detection apply(List<Object> detectionPb) {
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
              new Callable<List<Object>>() {
                @Override
                public List<Object> call() {
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
      List<List<Object>> detectionsPb =
          runWithRetries(
              new Callable<List<List<Object>>>() {
                @Override
                public List<List<Object>> call() {
                  return translateRpc.detect(texts);
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock());
      Iterator<List<Object>> detectionIterator = detectionsPb.iterator();
      Iterator<String> textIterator = texts.iterator();
      while (detectionIterator.hasNext() && textIterator.hasNext()) {
        List<Object> detectionPb = detectionIterator.next();
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
              new Callable<List<Object>>() {
                @Override
                public List<Object> call() {
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
