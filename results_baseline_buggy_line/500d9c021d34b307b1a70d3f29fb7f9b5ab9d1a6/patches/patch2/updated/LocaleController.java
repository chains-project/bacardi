package com.example.web;

import java.util.Locale;
import java.util.logging.Logger;
import jakarta.enterprise.context.RequestScoped; // Updated import
import jakarta.inject.Inject; // Updated import
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.mvc.MvcContext;
import jakarta.ws.rs.GET; // Updated import
import jakarta.ws.rs.Path; // Updated import

/**
 *
 * @author hantsy
 */
@Path("locale")
@Controller
@RequestScoped
public class LocaleController {

    @Inject
    MvcContext mvc;

    @Inject
    Models models;

    @Inject
    Logger log;

    @GET
    public String get() {
        Locale locale = mvc.getLocale();
        models.put("locale", locale);
        return "locale.xhtml";
    }

}