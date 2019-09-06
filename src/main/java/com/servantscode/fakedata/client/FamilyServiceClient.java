package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FamilyServiceClient extends AbstractServiceClient {

    //public FamilyServiceClient() { super("http://person-svc:8080/rest/family"); }
    public FamilyServiceClient() { super("/rest/family"); }

    private Map<String, Integer> idCache = new HashMap<>(16);

    public Map<String, Object> createOrUpdateFamily(Map<String, Object> data) {
        return data.containsKey("id")?
                updateFamily(data):
                createFamily(data);
    }

    public Map<String, Object> createFamily(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("surname"));
        else
            System.err.println("Failed to create family. Status: " + response.getStatus());

        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});
        idCache.put((String)resp.get("surname"), (Integer)resp.get("id"));
        return resp;
    }

    public Map<String, Object> updateFamily(Map<String, Object> data) {
        Response response = put(data);

        if(response.getStatus() == 200)
            System.out.println("Updated: " + data.get("surname"));
        else
            System.err.println("Failed to update family. Status: " + response.getStatus());

        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});
        idCache.put((String)resp.get("surname"), (Integer)resp.get("id"));
        return resp;
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

    public Map<String, Object> getFamily(int id) {
        Response response = get("/" + id);
        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }

    public int getFamilyId(String name) {
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

        //Protect against last name substrings
        if(!name.equals(results.get(0).get("surname")))
            return 0;

        int id = (int)results.get(0).get("id");

        idCache.put(name, id);
        return id;
    }
}
