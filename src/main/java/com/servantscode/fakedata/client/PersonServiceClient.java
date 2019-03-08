package com.servantscode.fakedata.client;

import com.fasterxml.jackson.core.type.TypeReference;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class PersonServiceClient extends AbstractServiceClient {

//    public PersonServiceClient() { super("http://person-svc:8080/rest/person"); }
    public PersonServiceClient() { super("/rest/person"); }

    public Map<String, Object> createPerson(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("name"));
        else
            System.err.println("Failed to create person. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }

    public int getPeopleCount() {
        Response response = get();
        Map<String, Object> searchResponse = response.readEntity(new GenericType<Map<String, Object>>(){});

        if(response.getStatus() == 200)
            System.out.println("Got peopleCount: " + searchResponse.get("totalResults"));
        else
            System.err.println("Failed to count people. Status: " + response.getStatus());

        return (Integer) searchResponse.get("totalResults");
    }
}
