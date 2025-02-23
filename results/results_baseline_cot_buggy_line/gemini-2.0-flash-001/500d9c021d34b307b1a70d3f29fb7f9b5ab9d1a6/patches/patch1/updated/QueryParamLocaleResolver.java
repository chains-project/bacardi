package com.example.web;
// https://www.mvc-spec.org/learn/cookbook/custom_localeresolver_en.html

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mvc.locale.LocaleResolver;
import jakarta.mvc.MvcContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Request;

/**
 * Resolver to get the {@link Locale} to use from the requests query param <i>lang</i>.
 *
 * In case there is no request param with this name, the {@link Locale} will be resolved by a higher prioritised
 * implementation.
 *
 * Example usage:
 * <pre>
 * {@code
 * # Use default locale
 * curl -X GET <your-url>
 *
 * # Set german locale by query param
 * curl -X GET <your-url>?lang=de-DE
 * }
 * </pre>
 *
 * @author Tobias Erdle
 */
@Priority(1)
@ApplicationScoped
public class QueryParamLocaleResolver implements LocaleResolver {

    @Inject
    Logger log;

    @Inject
    private MvcContext mvcContext;

    @Override
    public Locale resolveLocale() {
        UriInfo uriInfo = mvcContext.getUriInfo();
        final String queryLang = uriInfo
                .getQueryParameters()
                .getFirst("lang");
        log.log(Level.INFO, "QueryParamLocaleResolver::resolveLocale:lang:{0}", queryLang);
        return queryLang != null ? Locale.forLanguageTag(queryLang) : null;
    }
}