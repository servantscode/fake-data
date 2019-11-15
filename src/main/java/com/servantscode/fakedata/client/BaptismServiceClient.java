package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class BaptismServiceClient extends BaseServiceClient {

    public BaptismServiceClient() { super("/rest/sacrament/baptism"); }

    public Map<String, Object> createBaptism(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created baptism: " + ((Map<String,Object>)data.get("person")).get("id"));
        else
            System.err.println("Failed to create baptism. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
