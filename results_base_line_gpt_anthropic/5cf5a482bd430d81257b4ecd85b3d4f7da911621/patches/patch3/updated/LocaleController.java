package com.example.web;

import java.util.Locale;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped; // Fixed import for the newer dependency
import javax.inject.Inject; // Fixed import for the newer dependency
import javax.mvc.Controller; // Fixed import for the newer dependency
import javax.mvc.Models; // Fixed import for the newer dependency
import jakarta.mvc.MvcContext; // Updated import for the newer dependency
import javax.ws.rs.GET; // Fixed import for the newer dependency
import javax.ws.rs.Path; // Fixed import for the newer dependency

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