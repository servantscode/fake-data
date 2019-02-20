package com.servantscode.fakedata.client;

import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
        translateDates(data);
        return buildInvocation()
                .post(Entity.entity(data, MediaType.APPLICATION_JSON));
    }

    /*package*/ Response get(Map<String, Object>... params) {
        return buildInvocation(params).get();
    }

    // ----- Private -----
    private void translateDates(Map<String, Object> data) {
        data.entrySet().forEach( (entry) -> {
            Object obj = entry.getValue();
            if(obj instanceof ZonedDateTime) {
                entry.setValue(((ZonedDateTime)obj).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } else if(obj instanceof List) {
                List list = (List)obj;
                if(!list.isEmpty() && list.get(0) instanceof Map)
                    list.forEach((item) -> translateDates((Map<String, Object>)item));
            } else if(obj instanceof Map) {
                translateDates((Map<String, Object>)obj);
            }
        });
    }

    private Invocation.Builder buildInvocation(Map<String, Object>... optionalParams) {
        ensureLogin();

        WebTarget target = webTarget;

        if(optionalParams.length > 0) {
            Map<String, Object> params = optionalParams[0];
            for(Map.Entry<String, Object> entry: params.entrySet())
                target = target.property(entry.getKey(), entry.getValue());
        }

        return target
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token);
    }

    private void ensureLogin() {
        if(isEmpty(token)) {
//            WebTarget webTarget = client.target("http://permission-svc:8080/rest/login");
            WebTarget webTarget = client.target("http://localhost/rest/login");

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
