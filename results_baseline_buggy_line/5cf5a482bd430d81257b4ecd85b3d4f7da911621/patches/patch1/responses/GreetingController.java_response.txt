package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mvc.Controller;
import javax.mvc.Models;
import javax.mvc.binding.BindingResult;
import javax.mvc.binding.MvcBinding;
import javax.mvc.binding.ParamError;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response; // Updated import for response handling

/**
 *
 * @author hantsy
 */
@Path("greeting")
@Controller
@RequestScoped
public class GreetingController {

    @Inject
    BindingResult bindingResult;

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
    public Response post( // Changed return type to Response
            @FormParam("greeting")
            @MvcBinding
            @NotBlank String greeting) {
        if (bindingResult.isFailed()) {
            AlertMessage alert = AlertMessage.danger("Validation violations!");
            bindingResult.getAllErrors()
                    .stream()
                    .forEach((ParamError t) -> {
                        alert.addError(t.getParamName(), "", t.getMessage());
                    });
            models.put("errors", alert);
            log.info("mvc binding failed.");
            return Response.ok("greeting.xhtml").build(); // Updated to return Response
        }

        log.info("redirect to greeting page.");
        flashMessage.notify(AlertMessage.Type.success, "Message:" + greeting);
        return Response.seeOther(javax.ws.rs.core.UriBuilder.fromPath("greeting").build()).build(); // Updated to return Response
    }

}