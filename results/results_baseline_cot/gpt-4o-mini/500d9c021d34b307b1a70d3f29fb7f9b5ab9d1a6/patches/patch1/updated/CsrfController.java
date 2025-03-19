package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mvc.Controller;
import javax.mvc.Models;
import javax.mvc.security.CsrfProtected;
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
    Logger log;

    @Inject
    AlertMessage flashMessage;

    @GET
    public String get() {
        return "csrf.xhtml";
    }

    @POST
    @CsrfProtected
    public String post(
            @FormParam("greeting")
            @NotBlank String greeting) {
        AlertMessage alert = AlertMessage.danger("Validation violations!");
        log.info("redirect to greeting page.");
        flashMessage.notify(AlertMessage.Type.success, "Message:" + greeting);
        return "redirect:csrf";
    }

}