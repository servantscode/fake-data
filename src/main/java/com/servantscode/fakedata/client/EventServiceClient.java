package com.servantscode.fakedata.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventServiceClient extends AbstractServiceClient {

    public EventServiceClient() { super("/rest/event"); }

    public Map<String, Object> createEvent(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("title"));
        else {
            System.err.println("Failed to create event. Status: " + response.getStatus());
            try {
                System.err.println("Call failed: " + new ObjectMapper().writeValueAsString(data));
            } catch (JsonProcessingException e) {
                //Won't happen
            }
        }

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }

    public Map<String, Object> getEvent(String eventSearch) {
        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1);
        params.put("search", eventSearch);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        return results.get(0);
    }
}
