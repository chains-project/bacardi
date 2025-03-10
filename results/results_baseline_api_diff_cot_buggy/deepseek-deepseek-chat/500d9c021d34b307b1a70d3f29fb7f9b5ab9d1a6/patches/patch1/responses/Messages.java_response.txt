import java.util.ResourceBundle;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.faces.context.FacesContext;

/**
 * Provides I18n messages for the UI per request. To get the correct locale, the method {@link FacesContext#getViewRoot()}
 * is used. This method uses the built-in {@link javax.faces.context.FacesContext} of the used JSF Implementation.
 *
 * @author Tobias Erdle
 * @see FacesContext#getViewRoot()
 */
@RequestScoped
@Named("msg")
public class Messages {

    private static final String BASE_NAME = "messages";

    @Inject
    private FacesContext facesContext;

    /**
     * Get the assigned message to some key based on the {@link java.util.Locale} of the current request.
     *
     * @param key the message key to use
     * @return the correct translation assigned to the key for the request locale, a fallback translation or a
     * placeholder for unknown keys.
     */
    public final String get(final String key) {
        final ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, facesContext.getViewRoot().getLocale());

        return bundle.containsKey(key) ? bundle.getString(key) : formatUnknownKey(key);
    }

    private static String formatUnknownKey(final String key) {
        return String.format("???%s???", key);
    }
}
