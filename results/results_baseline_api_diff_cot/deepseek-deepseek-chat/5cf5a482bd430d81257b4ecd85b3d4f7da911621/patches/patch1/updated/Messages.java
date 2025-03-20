import java.util.ResourceBundle;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import jakarta.mvc.locale.LocaleResolver;
import jakarta.mvc.locale.LocaleResolverContext;
import jakarta.mvc.locale.LocaleResolverFactory;
import java.util.Locale;

@RequestScoped
@Named("msg")
public class Messages {

    private static final String BASE_NAME = "messages";

    @Inject
    private LocaleResolver localeResolver;

    /**
     * Get the assigned message to some key based on the {@link java.util.Locale} of the current request.
     *
     * @param key the message key to use
     * @return the correct translation assigned to the key for the request locale, a fallback translation or a
     * placeholder for unknown keys.
     */
    public final String get(final String key) {
        Locale locale = localeResolver.resolveLocale(new LocaleResolverContext() {
            @Override
            public Locale getDefaultLocale() {
                return Locale.getDefault();
            }

            @Override
            public Locale getLocale() {
                return Locale.getDefault();
            }
        });

        final ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale);

        return bundle.containsKey(key) ? bundle.getString(key) : formatUnknownKey(key);
    }

    private static String formatUnknownKey(final String key) {
        return String.format("???%s???", key);
    }
}