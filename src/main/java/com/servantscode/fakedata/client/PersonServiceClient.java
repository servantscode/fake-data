package com.servantscode.fakedata.client;

import org.servantscode.commons.StringUtils;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


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

    public int getPeopleCount(String... search) {
        HashMap<String, Object> params = new HashMap<>(4);
        params.put("families", true);
        params.put("count", 1);
        if(search != null && search.length > 0 && StringUtils.isSet(search[0]))
            params.put("search", search[0]);

        Response response = get(params);
        Map<String, Object> searchResponse = response.readEntity(new GenericType<Map<String, Object>>(){});

        if(response.getStatus() == 200)
            System.out.println("Got peopleCount: " + searchResponse.get("totalResults"));
        else
            System.err.println("Failed to count people. Status: " + response.getStatus());

        return (Integer) searchResponse.get("totalResults");
    }

    public Map<String, Object> getPerson(int offset, String... search) {
        HashMap<String, Object> params = new HashMap<>(4);
        params.put("families", true);
        params.put("count", 1);
        params.put("start", offset);
        if(search != null && search.length > 0 && StringUtils.isSet(search[0]))
            params.put("search", search[0]);

        Response response = get(params);
        Map<String, Object> searchResponse = response.readEntity(new GenericType<Map<String, Object>>(){});

        if(response.getStatus() == 200) {
            Map<String, Object> person = ((List<Map<String, Object>>) searchResponse.get("results")).get(0);
            System.out.println("Got person: " + person.get("name"));
            return person;
        } else {
            System.err.println("Failed to count people. Status: " + response.getStatus());
            throw new RuntimeException("Could not get the person you wanted.");
        }
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

    public List<Integer> getPersonIds(String query) {
        HashMap<String, Object> params = new HashMap<>(4);
        params.put("families", true);
        params.put("count", 0);
        params.put("search", query);

        Response response = get(params);
        Map<String, Object> searchResponse = response.readEntity(new GenericType<Map<String, Object>>(){});
        List<Map<String, Object>> results = (List<Map<String, Object>>) searchResponse.get("results");
        System.out.println("Found " + results.size() + " people for query: " + query);

        return results.stream().map((result) -> (int)result.get("id")).collect(toList());
    }
}
