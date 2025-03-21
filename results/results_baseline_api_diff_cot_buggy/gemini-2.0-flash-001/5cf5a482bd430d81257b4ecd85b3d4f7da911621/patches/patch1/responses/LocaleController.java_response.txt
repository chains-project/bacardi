package com.example.web;

import java.util.Locale;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.mvc.MvcContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 *
 * @author hantsy
 */
@Path("locale")
@Controller
@RequestScoped
public class LocaleController {

    @Inject
    Logger log;

    @Inject
    jakarta.mvc.Models models;

    @GET
    public String get() {
        Locale locale = Locale.getDefault();
        models.put("locale", locale);
        return "locale.xhtml";
    }

}
