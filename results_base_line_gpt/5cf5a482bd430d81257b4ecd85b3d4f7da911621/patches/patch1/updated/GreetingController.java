package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("greeting")
@RequestScoped
public class GreetingController {

    @Inject
    Logger log;

    @Context
    private UriInfo uriInfo;

    @GET
    public String get() {
        return "greeting.xhtml";
    }

    @POST
    public Response post(
            @FormParam("greeting")
            @NotBlank String greeting) {
        log.info("redirect to greeting page.");
        return Response.seeOther(uriInfo.getAbsolutePathBuilder().path("greeting").build()).build();
    }
}