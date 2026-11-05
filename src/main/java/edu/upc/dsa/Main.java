package edu.upc.dsa;

import io.swagger.jaxrs.config.BeanConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.net.URI;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static final String BASE_URI = "http://localhost:8080/dsaApp/";

    public static HttpServer startServer() throws IOException {
        return startServer(URI.create(BASE_URI));
    }

    public static HttpServer startServer(URI uri) throws IOException {
        ResourceConfig config = new ResourceConfig().packages("edu.upc.dsa.services")
                .register(MyExceptionMapper.class)
                .register(MoxyJsonFeature.class)
                .register(io.swagger.jaxrs.listing.ApiListingResource.class)
                .register(io.swagger.jaxrs.listing.SwaggerSerializers.class)
                .property(ServerProperties.WADL_FEATURE_DISABLE, true);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(uri, config, false);
        try {
            server.start();
            BeanConfig beanConfig = new BeanConfig();
            int port = server.getListeners().iterator().next().getPort();
            beanConfig.setHost(uri.getHost() + ":" + port);
            beanConfig.setBasePath(uri.getPath().replaceAll("/$", ""));
            beanConfig.setSchemes(new String[] {"http"});
            beanConfig.setResourcePackage("edu.upc.dsa.services");
            beanConfig.setTitle("REST API - Flota de drones");
            beanConfig.setDescription("Gestion de drones, pilotos, mantenimiento y planes de vuelo");
            beanConfig.setVersion("1.0.0");
            beanConfig.setScan(true);
            return server;
        } catch (IOException | RuntimeException e) {
            server.shutdownNow();
            throw e;
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        LOGGER.info("Documentacion Swagger: {}swagger.json", BASE_URI);
        LOGGER.info("Servicio REST en {}. Pulsa Enter para detenerlo.", BASE_URI);
        try {
            System.in.read();
        } finally {
            server.shutdownNow();
        }
    }
}
