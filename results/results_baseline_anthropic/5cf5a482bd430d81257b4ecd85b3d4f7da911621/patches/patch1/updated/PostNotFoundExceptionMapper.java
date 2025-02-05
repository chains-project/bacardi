package com.example.config;

import com.example.domain.TaskNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.mvc.Models;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author hantsy
 */
@Provider
public class PostNotFoundExceptionMapper implements ExceptionMapper<TaskNotFoundException> {

    @Inject Logger log;
    //private static Logger log = Logger.getLogger(PostNotFoundExceptionMapper.class.getName());

    // Update the import for Models to the correct package if necessary
    // If the Models class is not found, it may have been moved or renamed in the updated dependency.
    // Check the documentation for the new package structure.
    @Inject
    javax.mvc.Models models; // Ensure the correct import for Models

    @Override
    public Response toResponse(TaskNotFoundException exception) {
        log.log(Level.INFO, "handling exception : PostNotFoundException");
        models.put("error", exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity("error.xhtml").build();
    }

}