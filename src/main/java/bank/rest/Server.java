package bank.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.net.URISyntaxException;

public class Server {
    public static void main(String[] args) throws URISyntaxException {
        URI baseUri = new URI("http://localhost:9998/bank/");
        ResourceConfig rc = new ResourceConfig().packages("bank.rest");
        JdkHttpServerFactory.createHttpServer(baseUri, rc);
    }
}
