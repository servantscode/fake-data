package com.servantscode.fakedata.client;

import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.servantscode.commons.StringUtils.isEmpty;

public class AbstractServiceClient {

    private static String token = null;

    final private Client client;
    final private WebTarget webTarget;

    /*package*/ AbstractServiceClient(String targetUrl) {
        client = ClientBuilder.newClient(new ClientConfig().register(this.getClass()));
        webTarget = client.target(targetUrl);
    }

    /*package*/ Response post(Map<String, Object> data) {
        return buildInvocation()
                .post(Entity.entity(data, MediaType.APPLICATION_JSON));
    }

    /*package*/ Response get() {
        return buildInvocation().get();
    }


    // ----- Private -----

    private Invocation.Builder buildInvocation() {
        ensureLogin();

        return webTarget
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token);
    }

    private void ensureLogin() {
        if(isEmpty(token)) {
            WebTarget webTarget = client.target("http://permission-svc:8080/rest/login");

            Map<String, String> credentials = new HashMap<>();
            credentials.put("email", "greg@servantscode.org");
            credentials.put("password", "Z@!!enHasTh1s");

            Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN);
            Response response = invocationBuilder.post(Entity.entity(credentials, MediaType.APPLICATION_JSON));

            if (response.getStatus() != 200)
                System.err.println("Failed to login. Status: " + response.getStatus());

            token = response.readEntity(String.class);

            System.out.println("Logged in: " + token);
        }
    }
}
