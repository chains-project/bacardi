package com.example.web;

import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
    public Response post(
            @FormParam("greeting") String greeting) {
        if (greeting == null || greeting.trim().isEmpty()) {
            log.info("Validation violations!");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Validation violations!").build();
        }

        log.info("redirect to greeting page.");
        return Response.seeOther(javax.ws.rs.core.UriBuilder.fromPath("greeting").build()).build();
    }

}