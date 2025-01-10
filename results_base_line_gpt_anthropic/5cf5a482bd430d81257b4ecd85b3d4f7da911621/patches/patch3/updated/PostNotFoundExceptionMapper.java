package com.example.config;

import com.example.domain.TaskNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Import the correct class from the updated package
import org.springframework.web.servlet.ModelAndView; // Ensure this import is correct based on the new dependencies

@Provider
public class PostNotFoundExceptionMapper implements ExceptionMapper<TaskNotFoundException> {

    @Inject 
    Logger log;

    // Removed incorrect injection of ModelAndView and managed it properly
    private ModelAndView modelAndView = new ModelAndView("error"); // Instantiate ModelAndView correctly

    @Override
    public Response toResponse(TaskNotFoundException exception) {
        log.log(Level.INFO, "handling exception : PostNotFoundException");
        
        // Use the updated modelAndView to put the error message
        modelAndView.addObject("error", exception.getMessage()); // Using addObject instead of put
        
        return Response.status(Response.Status.NOT_FOUND).entity("error.xhtml").build();
    }
}