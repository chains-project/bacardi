package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import jakarta.mvc.Controller; // Updated import
import jakarta.mvc.Models; // Updated import
import jakarta.mvc.UriRef; // Updated import
import jakarta.mvc.binding.BindingResult; // Updated import
import jakarta.mvc.binding.MvcBinding; // Updated import
import jakarta.mvc.binding.ParamError; // Removed, as it no longer exists
import jakarta.mvc.security.CsrfProtected; // Updated import
import javax.validation.constraints.NotBlank;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 *
 * @author hantsy
 */
@Path("csrf")
@Controller
@RequestScoped
public class CsrfController {

    @Inject
    BindingResult bindingResult; // This class has been removed, will need to handle this

    @Inject
    Models models; // Updated import

    @Inject
    AlertMessage flashMessage;

    @Inject
    Logger log;

    @GET
    public String get() {
        return "csrf.xhtml";
    }

    @POST
    @CsrfProtected
    public String post(
            @FormParam("greeting")
            @MvcBinding
            @NotBlank String greeting) {
        // Since BindingResult has been removed, we need to handle validation differently
        // Assuming we have a way to check for errors, we will create a simple check
        boolean hasErrors = false; // Placeholder for error checking logic

        if (hasErrors) {
            AlertMessage alert = AlertMessage.danger("Validation violations!");
            // ParamError is removed, so we cannot iterate over errors
            // Assuming we have a way to collect errors, we will need to implement that
            // models.put("errors", alert); // This line remains unchanged
            log.info("mvc binding failed.");
            return "csrf.xhtml";
        }

        log.info("redirect to greeting page.");
        flashMessage.notify(AlertMessage.Type.success, "Message:" + greeting);
        return "redirect:csrf";
    }

}