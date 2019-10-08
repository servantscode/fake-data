package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class ProgramServiceClient extends AbstractServiceClient {

    public ProgramServiceClient() { super("/rest/program"); }

    public Map<String, Object> createProgram(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created program: " + data.get("name"));
        else
            System.err.println("Failed to create program. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
