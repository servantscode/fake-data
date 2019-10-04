package com.servantscode.fakedata.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.servantscode.commons.StringUtils.isEmpty;

public class AbstractServiceClient {

    private static String token = null;

    final private Client client;
    final private WebTarget webTarget;

    private static String urlPrefix = "http://localhost";

    /*package*/ AbstractServiceClient(String service) {
        client = ClientBuilder.newClient(new ClientConfig().register(this.getClass()));
        webTarget = client.target(urlForService(service));
    }

    /*package*/ Response post(Map<String, Object> data, Map<String, Object>... params) {
        try {
            translateDates(data);
            return buildInvocation(params)
                    .post(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    /*package*/ Response post(List<Map<String, Object>> data, Map<String, Object>... params) {
        try {
            data.forEach(this::translateDates);
            return buildInvocation(params)
                    .post(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    /*package*/ Response put(Map<String, Object> data, Map<String, Object>... params) {
        try {
            translateDates(data);
            return buildInvocation(params)
                    .put(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    /*package*/ Response put(List<Map<String, Object>> data, Map<String, Object>... params) {
        try {
            data.forEach(this::translateDates);
            return buildInvocation(params)
                    .put(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (Throwable e) {
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e1) {
                System.err.println("Won't happen");
            }
            throw new RuntimeException("Call failed: ", e);
        }
    }

    /*package*/ Response get(Map<String, Object>... params) {
        return buildInvocation(params).get();
    }

    /*package*/ Response get(String path, Map<String, Object>... params) {
        return buildInvocation(path, params).get();
    }

    /*package*/ Response delete(int id, Map<String, Object>... params) {
        return buildInvocation("/" + id, params).delete();
    }

    // ----- Private -----
    private static String urlForService(String resource) {
        return urlPrefix + resource;
    }

    public static void setUrlPrefix(String prefix) {
        urlPrefix = prefix;
    }

    private void translateDates(Map<String, Object> data) {
        data.entrySet().forEach( (entry) -> {
            Object obj = entry.getValue();
            if(obj instanceof ZonedDateTime) {
                entry.setValue(((ZonedDateTime) obj).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } else if(obj instanceof LocalDate) {
                entry.setValue(((LocalDate)obj).format(DateTimeFormatter.ISO_DATE));
            } else if(obj instanceof List) {
                List list = (List)obj;
                if(!list.isEmpty() && list.get(0) instanceof Map)
                    list.forEach((item) -> translateDates((Map<String, Object>)item));
                if(!list.isEmpty() && list.get(0) instanceof LocalDate)
                    entry.setValue(list.stream()
                            .map((item) -> ((LocalDate)item).format(DateTimeFormatter.ISO_DATE)).collect(Collectors.toList())
                    );
            } else if(obj instanceof Map) {
                translateDates((Map<String, Object>)obj);
            }
        });
    }

    private Invocation.Builder buildInvocation(String path, Map<String, Object>... optionalParams) {
        return buildInvocation(webTarget.path(path), optionalParams);
    }

    private Invocation.Builder buildInvocation(Map<String, Object>... optionalParams) {
        return buildInvocation(webTarget, optionalParams);
    }

    private Invocation.Builder buildInvocation(WebTarget target, Map<String, Object>... optionalParams) {
        ensureLogin();

        if(optionalParams.length > 0) {
            Map<String, Object> params = optionalParams[0];
            for(Map.Entry<String, Object> entry: params.entrySet())
                target = target.queryParam(entry.getKey(), entry.getValue());
        }

        return target
                .request(MediaType.APPLICATION_JSON)
                .header("referer", urlPrefix)
                .header("Authorization", "Bearer " + token);
    }

    private void ensureLogin() {
        if(isEmpty(token))
            login("greg@servantscode.org","1234");
    }

    public static void login(String email, String password) {
        WebTarget webTarget = ClientBuilder.newClient(new ClientConfig().register(AbstractServiceClient.class))
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
