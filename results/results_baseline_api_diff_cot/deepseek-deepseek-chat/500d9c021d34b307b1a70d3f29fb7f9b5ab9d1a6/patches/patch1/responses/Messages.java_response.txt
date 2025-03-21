import java.util.ResourceBundle;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mvc.locale.LocaleResolver;
import javax.mvc.locale.LocaleResolverContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

@RequestScoped
@Named("msg")
public class Messages {

    private static final String BASE_NAME = "messages";

    @Inject
    private LocaleResolver localeResolver;

    @Inject
    private HttpHeaders httpHeaders;

    @Inject
    private UriInfo uriInfo;

    @Inject
    private Request request;

    @Inject
    private Providers providers;

    @Inject
    private Configuration configuration;

    public final String get(final String key) {
        LocaleResolverContext context = new LocaleResolverContext() {
            @Override
            public HttpHeaders getHttpHeaders() {
                return httpHeaders;
            }

            @Override
            public UriInfo getUriInfo() {
                return uriInfo;
            }

            @Override
            public Request getRequest() {
                return request;
            }

            @Override
            public Providers getProviders() {
                return providers;
            }

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