package com.wire.lithium.server.monitoring;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Api
@Path("/status")
@Produces(MediaType.TEXT_PLAIN)
public class StatusResource {
    @GET
    @ApiOperation(value = "Status")
    public Response statusEmpty() {
        return Response
                .ok()
                .build();
    }
}