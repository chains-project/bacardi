import java.util.ResourceBundle;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
//https://www.mvc-spec.org/learn/cookbook/multilang_en.html
/**
 * Provides I18n messages for the UI per request. To get the correct locale, the method {@link HttpServletRequest#getLocale()}
 * is used. This method uses the built-in {@link java.util.Locale} resolution of the servlet container.
 *
 * @author Tobias Erdle
 * @see HttpServletRequest#getLocale()
 */
@RequestScoped
@Named("msg")
public class Messages {

    private static final String BASE_NAME = "messages";

    @Inject
    private HttpServletRequest request;

    /**
     * Get the assigned message to some key based on the {@link java.util.Locale} of the current request.
     *
     * @param key the message key to use
     * @return the correct translation assigned to the key for the request locale, a fallback translation or a
     * placeholder for unknown keys.
     */
    public final String get(final String key) {
        final ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, request.getLocale());

        return bundle.containsKey(key) ? bundle.getString(key) : formatUnknownKey(key);
    }

    private static String formatUnknownKey(final String key) {
        return String.format("???%s???", key);
    }
}
