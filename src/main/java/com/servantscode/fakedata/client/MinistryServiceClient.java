package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MinistryServiceClient extends BaseServiceClient {

    private Map<String, Integer> ministryIdCache = new HashMap<>(16);
    private boolean ministriesLoaded = false;

    public MinistryServiceClient() { super("/rest/ministry"); }

    public Map<String, Object> createMinistry(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("name"));
        else {
            System.err.println("Failed to create ministry. Status: " + response.getStatus());
            return null;
        }

        return  response.readEntity(new GenericType<Map<String, Object>>(){});
    }

    public int getMinistryId(String ministryName) {
        if(ministryIdCache.get(ministryName) != null)
            return ministryIdCache.get(ministryName);

        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1);
        params.put("search", ministryName);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        int id = (int)results.get(0).get("id");

        ministryIdCache.put(ministryName, id);
        return id;
    }

    public List<String> getMinistries() {
        if(ministriesLoaded)
            return new ArrayList<>(ministryIdCache.keySet());

        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1000);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        results.forEach((result) -> ministryIdCache.put((String)result.get("name"), (Integer)result.get("id")));
        ministriesLoaded = true;
        return results.stream().map((result) -> (String)result.get("name")).collect(Collectors.toList());
    }
}
