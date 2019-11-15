package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class ProgramGroupServiceClient extends BaseServiceClient {

    public ProgramGroupServiceClient() { super("/rest/program/group"); }

    public Map<String, Object> createProgramGroup(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created program group: " + data.get("name"));
        else
            System.err.println("Failed to create program group. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
