package com.example.config;

import com.example.domain.TaskNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Import the updated class from the correct package
import javax.mvc.ModelAndView;

@Provider
public class PostNotFoundExceptionMapper implements ExceptionMapper<TaskNotFoundException> {

    @Inject 
    Logger log;

    // Replace Models with ModelAndView to match the updated dependency
    @Inject
    ModelAndView modelAndView;

    @Override
    public Response toResponse(TaskNotFoundException exception) {
        log.log(Level.INFO, "handling exception : PostNotFoundException");
        
        // Use the updated modelAndView to put the error message
        modelAndView.put("error", exception.getMessage());
        
        return Response.status(Response.Status.NOT_FOUND).entity("error.xhtml").build();
    }
}