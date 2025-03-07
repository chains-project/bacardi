package com.example.web;

import java.util.Locale;
import java.util.logging.Logger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.mvc.annotation.Controller;
import jakarta.mvc.Models;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 *
 * @author hantsy
 */
@Path("locale")
@Controller
@RequestScoped
public class LocaleController {

    @Inject
    Models models;

    @Inject
    Logger log;

    @GET
    public String get() {
        Locale locale = Locale.getDefault(); // Assuming MvcContext.getLocale() is replaced with Locale.getDefault()
        models.put("locale", locale);
        return "locale.xhtml";
    }

}