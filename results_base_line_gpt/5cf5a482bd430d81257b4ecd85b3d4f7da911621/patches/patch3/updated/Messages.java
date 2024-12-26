import java.util.ResourceBundle;
import java.util.Locale;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
// Updated import for LocaleResolver to a compatible library for localization
import org.springframework.web.servlet.LocaleResolver; // Updated import
import org.springframework.web.servlet.support.RequestContextUtils; // Added import for request context utils

/**
 * Provides I18n messages for the UI per request. To get the correct locale, the method {@link MvcContext#getLocale()}
 * is used. This method uses the built-in {@link javax.mvc.locale.LocaleResolver} of the used MVC Implementation.
 *
 * @author Tobias Erdle
 * @see MvcContext#getLocale()
 * @see javax.mvc.locale.LocaleResolver
 */
@RequestScoped
@Named("msg")
public class Messages {

    private static final String BASE_NAME = "messages";

    private Locale locale; // Added a Locale variable
    @Inject
    private LocaleResolver localeResolver; // Updated to use LocaleResolver

    /**
     * Get the assigned message to some key based on the {@link java.util.Locale} of the current request.
     *
     * @param key the message key to use
     * @return the correct translation assigned to the key for the request locale, a fallback translation or a
     * placeholder for unknown keys.
     */
    public final String get(final String key) {
        locale = localeResolver.resolveLocale(RequestContextUtils.getRequest()); // Updated to use LocaleResolver
        final ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale);

        return bundle.containsKey(key) ? bundle.getString(key) : formatUnknownKey(key);
    }

    private static String formatUnknownKey(final String key) {
        return String.format("???%s???", key);
    }
}