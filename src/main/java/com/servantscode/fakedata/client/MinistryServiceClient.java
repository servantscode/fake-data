package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;

public class MinistryServiceClient extends AbstractServiceClient {

    public MinistryServiceClient() {
        super("http://localhost:81/rest/ministry");
    }

    public Map<String, Object> createMinistry(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("name"));
        else {
            System.err.println("Failed to create ministry. Status: " + response.getStatus());
            return null;
        }

        return  response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
