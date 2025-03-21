package com.example.web;
// https://www.mvc-spec.org/learn/cookbook/custom_localeresolver_en.html

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import jakarta.mvc.event.AfterControllerEvent;
import jakarta.mvc.event.BeforeControllerEvent;
import jakarta.mvc.event.ControllerRedirectEvent;
import jakarta.mvc.engine.ViewEngineContext;
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
public class QueryParamLocaleResolver {
    
    @Inject
    Logger log;
    
    public Locale resolveLocale(final BeforeControllerEvent event) {
        final UriInfo uriInfo = event.getUriInfo();
        final String queryLang = uriInfo.getQueryParameters().getFirst("lang");
        log.log(Level.INFO, "QueryParamLocaleResolver::resolveLocale:lang:{0}", queryLang);
        return queryLang != null ? Locale.forLanguageTag(queryLang) : null;
    }
}