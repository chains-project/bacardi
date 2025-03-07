import java.util.ResourceBundle;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mvc.locale.LocaleResolver;
import javax.mvc.locale.LocaleResolverFactory;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;

/**
 * Provides I18n messages for the UI per request. To get the correct locale, the method {@link LocaleResolver#resolveLocale()} 
 * is used. This method uses the built-in {@link javax.mvc.locale.LocaleResolver} of the used MVC Implementation.
 *
 * @author Tobias Erdle
 * @see LocaleResolver#resolveLocale()
 * @see javax.mvc.locale.LocaleResolver
 */
@RequestScoped
@Named("msg")
public class Messages {

    private static final String BASE_NAME = "messages";

    @Inject
    private LocaleResolverFactory localeResolverFactory;

    @Context
    private Configuration configuration;

    /**
     * Get the assigned message to some key based on the {@link java.util.Locale} of the current request.
     *
     * @param key the message key to use
     * @return the correct translation assigned to the key for the request locale, a fallback translation or a
     * placeholder for unknown keys.
     */
    public final String get(final String key) {
        LocaleResolver localeResolver = localeResolverFactory.getLocaleResolver(configuration);
        final ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, localeResolver.resolveLocale());

        return bundle.containsKey(key) ? bundle.getString(key) : formatUnknownKey(key);
    }

    private static String formatUnknownKey(final String key) {
        return String.format("???%s???", key);
    }
}