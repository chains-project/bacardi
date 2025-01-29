package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 *
 * @author hantsy
 */
@Path("greeting")
@RequestScoped
public class GreetingController {

    @Inject
    Logger log;

    @GET
    public String get() {
        return "greeting.xhtml";
    }

    @POST
    public String post(
            @FormParam("greeting")
            String greeting) {
        if (greeting == null || greeting.isBlank()) {
            log.info("Validation violations!");
            return "greeting.xhtml";
        }

        log.info("redirect to greeting page.");
        return "redirect:greeting";
    }

}