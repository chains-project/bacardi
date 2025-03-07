package com.example.web;

import java.util.logging.Logger;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.mvc.security.CsrfProtected;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

/**
 *
 * @author hantsy
 */
@Path("greeting")
@Controller
@RequestScoped
public class GreetingController {

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
    @CsrfProtected
    public String post(
            @FormParam("greeting")
            @NotBlank String greeting) {
        // Assuming BindingResult is no longer available, we directly check the greeting for validation.
        if (greeting == null || greeting.trim().isEmpty()) {
            AlertMessage alert = AlertMessage.danger("Validation voilations!");
            alert.addError("greeting", "", "Greeting cannot be blank");
            models.put("errors", alert);
            log.info("Validation failed.");
            return "greeting.xhtml";
        }

        log.info("redirect to greeting page.");
        flashMessage.notify(AlertMessage.Type.success, "Message:" + greeting);
        return "redirect:greeting";
    }

}