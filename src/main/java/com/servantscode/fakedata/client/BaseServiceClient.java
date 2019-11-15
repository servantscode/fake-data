package com.servantscode.fakedata.client;

import org.glassfish.jersey.client.ClientConfig;
import org.servantscode.commons.client.AbstractServiceClient;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.servantscode.commons.StringUtils.isEmpty;

public class BaseServiceClient extends AbstractServiceClient {

    private static String token = null;

    private static String urlPrefix = "http://localhost";

    /*package*/ BaseServiceClient(String service) {
        super(urlForService(service));
    }

    @Override
    public String getReferralUrl() {
        return urlPrefix;
    }

    @Override
    public String getAuthorization() {
        //If not logged in use default development credentials
        if(isEmpty(token))
            login("greg@servantscode.org","1234");

        return "Bearer " + token;
    }

    // ----- Private -----
    private static String urlForService(String resource) {
        return urlPrefix + resource;
    }

    public static void setUrlPrefix(String prefix) {
        urlPrefix = prefix;
    }

    public static void login(String email, String password) {
        WebTarget webTarget = ClientBuilder.newClient(new ClientConfig().register(BaseServiceClient.class))
                .target(urlForService("/rest/login"));

        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);
        Response response = invocationBuilder
                .header("referer", urlPrefix)
                .post(Entity.entity(credentials, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200)
            System.err.println("Failed to login. Status: " + response.getStatus());

        token = response.readEntity(String.class);
        System.out.println("Logged in: " + token);
    }
}
