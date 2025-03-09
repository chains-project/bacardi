import javax.mvc.locale.LocaleResolver;
import javax.mvc.locale.LocaleResolverContext;

public class QueryParamLocaleResolver implements LocaleResolver {
    @Override
    public Locale resolveLocale(final LocaleResolverContext context) {
        final String queryLang = context.getUriInfo()
                .getQueryParameters()
                .getFirst("lang");
        return queryLang != null ? Locale.forLanguageTag(queryLang) : null;
    }
}