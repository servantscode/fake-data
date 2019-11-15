package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class ConfirmationServiceClient extends BaseServiceClient {

    public ConfirmationServiceClient() { super("/rest/sacrament/confirmation"); }

    public Map<String, Object> createConfirmation(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created confirmation: " + ((Map<String,Object>)data.get("person")).get("id"));
        else
            System.err.println("Failed to create confirmation. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
