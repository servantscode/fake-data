package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MinistryRoleServiceClient extends AbstractServiceClient {

    private Map<String, Integer> roleIdCache = new HashMap<>(8);

    public MinistryRoleServiceClient(int ministryId) { super("/rest/ministry/" + ministryId + "/role"); }

    public Map<String, Object> createMinistryRole(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("name"));
        else {
            System.err.println("Failed to create ministry role. Status: " + response.getStatus());
            return null;
        }

        return  response.readEntity(new GenericType<Map<String, Object>>(){});
    }

    public int getMinistryRoleId(String roleName) {
        if(roleIdCache.get(roleName) != null)
            return roleIdCache.get(roleName);

        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1);
        params.put("partial_name", roleName);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        int id = (int)results.get(0).get("id");

        roleIdCache.put(roleName, id);
        return id;
    }
}
