package com.example.web;

import java.util.logging.Logger;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
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
    private BindingResult bindingResult;

    @Inject
    private Models models;

    @Inject
    private AlertMessage flashMessage;

    @Inject
    private Logger log;

    @GET
    public String get() {
        return "greeting.xhtml";
    }

    @POST
    public String post(
            @FormParam("greeting")
            @NotBlank String greeting) {
        if (bindingResult.isFailed()) {
            AlertMessage alert = AlertMessage.danger("Validation voilations!");
            bindingResult.getAllErrors()
                    .stream()
                    .forEach((ParamError t) -> {
                        alert.addError(t.getParamName(), "", t.getMessage());
                    });
            models.put("errors", alert);
            log.info("mvc binding failed.");
            return "greeting.xhtml";
        }

        log.info("redirect to greeting page.");
        flashMessage.notify(AlertMessage.Type.success, "Message:" + greeting);
        return "redirect:greeting";
    }

    // Assuming BindingResult, Models, and ParamError are custom classes or interfaces
    // that are part of the application's internal logic and are not part of the removed dependency.
    // If they are part of the removed dependency, they would need to be replaced or implemented.
}