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
import javax.validation.constraints.NotBlank;

/**
 *
 * @author hantsy
 */
@Path("greeting")
@Controller
@RequestScoped
public class GreetingController {

    // Removed BindingResult and Models due to API changes
    // Added new imports for handling validation and model attributes
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
        // Simulating validation check
        if (greeting == null || greeting.isEmpty()) {
            log.info("mvc binding failed.");
            return "greeting.xhtml";
        }

        log.info("redirect to greeting page.");
        // Simulating flash message notification
        return "redirect:greeting";
    }

}