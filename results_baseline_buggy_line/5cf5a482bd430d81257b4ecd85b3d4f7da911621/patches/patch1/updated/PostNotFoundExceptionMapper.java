package com.example.config;

import com.example.domain.TaskNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Import the correct Models class based on the updated dependency
import javax.mvc.Models; // Ensure this import is correct based on the new dependency

/**
 *
 * @author hantsy
 */
@Provider
public class PostNotFoundExceptionMapper implements ExceptionMapper<TaskNotFoundException> {

    @Inject Logger log;

    // Updated Models variable to ensure it is correctly initialized
    @Inject
    Models models;

    @Override
    public Response toResponse(TaskNotFoundException exception) {
        log.log(Level.INFO, "handling exception : PostNotFoundException");
        models.put("error", exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity("error.xhtml").build();
    }

}