package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PersonServiceClient extends AbstractServiceClient {

    public PersonServiceClient() { super("/rest/person"); }

    private Map<String, Integer> idCache = new HashMap<>(16);

    public Map<String, Object> createPerson(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("name"));
        else
            System.err.println("Failed to create person. Status: " + response.getStatus());

        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});
        idCache.put((String)resp.get("name"), (Integer)resp.get("id"));
        return resp;
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

    public int getPersonId(String name) {
        if(idCache.get(name) != null)
            return idCache.get(name);

        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1);
        params.put("search", name);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        if(results.size() == 0)
            return 0;

        int id = (int)results.get(0).get("id");

        idCache.put(name, id);
        return id;
    }
}
