package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class FamilyServiceClient extends AbstractServiceClient {

    public FamilyServiceClient() {
        super("http://person-svc/rest/family");
    }

    public int getFamilyCount() {
        Response response = get();

        if(response.getStatus() != 200) {
            System.err.println("Failed to count families. Status: " + response.getStatus());
            throw new RuntimeException("Could not count families");
        }

        Map<String, Object> searchResponse = response.readEntity(new GenericType<Map<String, Object>>(){});
        if(response.getStatus() == 200)
            System.out.println("Got familyCount: " + searchResponse.get("totalResults"));

        return (Integer) searchResponse.get("totalResults");
    }
}
