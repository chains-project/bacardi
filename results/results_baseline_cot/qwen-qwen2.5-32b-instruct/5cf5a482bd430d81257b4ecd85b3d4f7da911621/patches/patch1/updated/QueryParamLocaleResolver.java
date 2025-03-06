package com.example.web;
// https://www.mvc-spec.org/learn/cookbook/custom_localeresolver_en.html

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import jakarta.mvc.locale.LocaleResolver; // Updated import
import jakarta.mvc.locale.LocaleResolverContext; // Updated import
import jakarta.ws.rs.core.UriInfo; // Updated import

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
    
    public Locale resolveLocale(final LocaleResolverContext context) { // Removed @Override
        final String queryLang = context.getUriInfo()
                .getQueryParameters()
                .getFirst("lang");
        log.log(Level.INFO, "QueryParamLocaleResolver::resolveLocale:lang:{0}", queryLang);
        return queryLang != null ? Locale.forLanguageTag(queryLang) : null;
    }
}