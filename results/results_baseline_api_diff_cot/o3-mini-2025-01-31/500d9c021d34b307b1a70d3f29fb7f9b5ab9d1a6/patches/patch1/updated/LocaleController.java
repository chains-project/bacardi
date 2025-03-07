package com.example.web;

import java.util.Locale;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
        Locale locale = Locale.getDefault();
        models.put("locale", locale);
        return "locale.xhtml";
    }
}