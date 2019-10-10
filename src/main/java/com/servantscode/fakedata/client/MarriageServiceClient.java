package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class MarriageServiceClient extends AbstractServiceClient {

    public MarriageServiceClient() { super("/rest/sacrament/marriage"); }

    public Map<String, Object> createMarriage(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created marriage: " + ((Map<String,Object>)data.get("groom")).get("id") + " - " + ((Map<String,Object>)data.get("bride")).get("id"));
        else
            System.err.println("Failed to create marriage. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
