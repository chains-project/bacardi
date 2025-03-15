package com.example.web;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.mvc.UriRef;
import jakarta.mvc.binding.MvcBinding;
import jakarta.mvc.security.CsrfProtected;

@Path("greeting")
@Controller
@RequestScoped
public class GreetingController {

    @Inject
    BindingResult bindingResult = new DummyBindingResult();

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
    @UriRef("greeting-post")
    public String post(
            @FormParam("greeting")
            @MvcBinding
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

    private static class DummyBindingResult implements BindingResult {
        @Override
        public boolean isFailed() {
            return false;
        }
        @Override
        public List<ParamError> getAllErrors() {
            return Collections.emptyList();
        }
    }
    
    public static interface BindingResult {
        boolean isFailed();
        List<ParamError> getAllErrors();
    }
    
    public static interface ParamError {
        String getParamName();
        String getMessage();
    }
}