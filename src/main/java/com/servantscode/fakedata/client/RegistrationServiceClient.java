package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class RegistrationServiceClient extends AbstractServiceClient {

    public RegistrationServiceClient(int id) { super(String.format("/rest/program/%d/registration", id)); }

    public Map<String, Object> createRegistration(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created program registration: " + data.get("name"));
        else
            System.err.println("Failed to create program registration. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
