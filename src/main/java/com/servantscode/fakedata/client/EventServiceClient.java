package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;

public class EventServiceClient extends AbstractServiceClient {

    public EventServiceClient() { super("/rest/event"); }

    public Map<String, Object> createEvent(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("description"));
        else
            System.err.println("Failed to create event. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
