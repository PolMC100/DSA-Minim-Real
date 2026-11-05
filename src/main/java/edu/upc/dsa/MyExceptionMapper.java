package edu.upc.dsa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MyExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOGGER = LogManager.getLogger(MyExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        int status = exception instanceof WebApplicationException
                ? ((WebApplicationException) exception).getResponse().getStatus()
                : exception instanceof IllegalArgumentException ? 400
                : exception instanceof IllegalStateException ? 409 : 500;
        String message = exception.getMessage();
        if (status == 500) {
            LOGGER.error("Error inesperado en el servicio REST", exception);
            message = "Error interno del servidor";
        } else {
            LOGGER.error("Peticion rechazada ({}): {}", status, message);
        }
        Response.ResponseBuilder response = exception instanceof WebApplicationException
                ? Response.fromResponse(((WebApplicationException) exception).getResponse())
                : Response.status(status);
        return response.type(MediaType.TEXT_PLAIN).entity(message).build();
    }
}
