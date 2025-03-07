import java.util.ResourceBundle;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mvc.locale.LocaleResolver;
import javax.mvc.locale.LocaleResolverContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;

@RequestScoped
@Named("msg")
public class Messages {

    private static final String BASE_NAME = "messages";

    @Inject
    private LocaleResolver localeResolver;

    @Context
    private Configuration configuration;

    public final String get(final String key) {
        LocaleResolverContext context = new LocaleResolverContext() {
            @Override
            public Configuration getConfiguration() {
                return configuration;
            }
        };
        final ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, localeResolver.resolveLocale(context));

        return bundle.containsKey(key) ? bundle.getString(key) : formatUnknownKey(key);
    }

    private static String formatUnknownKey(final String key) {
        return String.format("???%s???", key);
    }
}