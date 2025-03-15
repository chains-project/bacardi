package com.example.web;

import java.util.Locale;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.mvc.MvcContext;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

/**
 *
 * @author hantsy
 */
@Path("locale")
@Controller
@RequestScoped
public class LocaleController {

    @Context
    HttpServletRequest request;

    @Inject
    Models models;

    @Inject
    Logger log;

    @GET
    public String get() {
        Locale locale = request.getLocale();
        models.put("locale", locale);
        return "locale.xhtml";
    }

}