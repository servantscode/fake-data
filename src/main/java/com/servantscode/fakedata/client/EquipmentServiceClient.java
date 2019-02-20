package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EquipmentServiceClient extends AbstractServiceClient {

    private Map<String, Integer> equipmentIdCache = new HashMap<>(16);
    private boolean equipmentLoaded = false;

//    public EquipmentServiceClient() { super("http://schedule-svc:8080/rest/equipment"); }
    public EquipmentServiceClient() { super("http://localhost/rest/equipment"); }

    public Map<String, Object> createEquipment(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("name"));
        else
            System.err.println("Failed to create equipment. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }

    public int getEquipmentId(String equipmentName) {
        if(equipmentIdCache.get(equipmentName) != null)
            return equipmentIdCache.get(equipmentName);

        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1);
        params.put("partial_name", equipmentName);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        int id = (int)results.get(0).get("id");

        equipmentIdCache.put(equipmentName, id);
        return id;
    }

    public Set<String> getEquipment() {
        if(!equipmentLoaded)
            loadAllEquipment();

        return equipmentIdCache.keySet();
    }

    // ----- Private -----

    private List<Map<String, Object>> loadAllEquipment() {
        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1000);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        results.stream().forEach((result) -> equipmentIdCache.put((String)result.get("name"), (Integer)result.get("id")));
        equipmentLoaded = true;
        return results;
    }
}
