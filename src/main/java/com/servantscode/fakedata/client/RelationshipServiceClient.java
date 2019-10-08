package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RelationshipServiceClient extends AbstractServiceClient {

    public RelationshipServiceClient() { super("/rest/relationship"); }

    private Map<String, Integer> idCache = new HashMap<>(16);

    public void createRelationships(List<Map<String, Object>> data, boolean createReciprocals) {
        HashMap<String, Object> params = new HashMap<>(2);
        params.put("addReciprocals", createReciprocals);

        Response response = put(data, params);

        if(response.getStatus() == 204)
            System.out.println("Created relationships");
        else
            System.err.println("Failed to create relationship. Status: " + response.getStatus());
    }
}
