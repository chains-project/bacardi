package com.example.web;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mvc.MvcContext;
import javax.ws.rs.core.UriInfo;

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
public class QueryParamLocaleResolver { // Removed LocaleResolver interface

    @Inject
    Logger log;

    public Locale resolveLocale(final MvcContext context) { // Updated parameter type
        final String queryLang = context.getUriInfo()
                .getQueryParameters()
                .getFirst("lang");
        log.log(Level.INFO, "QueryParamLocaleResolver::resolveLocale:lang:{0}", queryLang);
        return queryLang != null ? Locale.forLanguageTag(queryLang) : null;
    }
}