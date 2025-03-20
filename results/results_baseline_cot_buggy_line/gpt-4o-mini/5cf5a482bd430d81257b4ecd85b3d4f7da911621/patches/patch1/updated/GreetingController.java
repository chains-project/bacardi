package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mvc.Controller;
import javax.mvc.Models;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 *
 * @author hantsy
 */
@Path("greeting")
@Controller
@RequestScoped
public class GreetingController {

    // Removed BindingResult and MvcBinding due to API changes
    // Replaced with a simple validation check
    @Inject
    Models models;

    @Inject
    AlertMessage flashMessage;

    @Inject
    Logger log;

    @GET
    public String get() {
        return "greeting.xhtml";
    }

    @POST
    public String post(
            @FormParam("greeting")
            @NotBlank String greeting) {
        // Simple validation check instead of using BindingResult
        if (greeting == null || greeting.trim().isEmpty()) {
            AlertMessage alert = AlertMessage.danger("Validation violations!");
            alert.addError("greeting", "", "Greeting must not be blank.");
            models.put("errors", alert);
            log.info("mvc binding failed.");
            return "greeting.xhtml";
        }

        log.info("redirect to greeting page.");
        flashMessage.notify(AlertMessage.Type.success, "Message:" + greeting);
        return "redirect:greeting";
    }

}