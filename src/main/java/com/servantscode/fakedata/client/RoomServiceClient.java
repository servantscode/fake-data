package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

public class RoomServiceClient extends AbstractServiceClient {

    private Map<String, Integer> roomIdCache = new HashMap<>(16);
    private boolean roomsLoaded = false;

    public RoomServiceClient() {
        super("http://schedule-svc:84/rest/room");
    }

    public Map<String, Object> createRoom(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created: " + data.get("name"));
        else
            System.err.println("Failed to create room. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }

    public int getRoomId(String roomName) {
        if(roomIdCache.get(roomName) != null)
            return roomIdCache.get(roomName);

        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1);
        params.put("partial_name", roomName);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        int id = (int)results.get(0).get("id");

        roomIdCache.put(roomName, id);
        return id;
    }

    public List<String> getClassRooms() {
        List<Map<String, Object>> results = loadAllRooms();
        return results.stream().filter((result) -> result.get("type").equals("CLASS")).map((result) -> (String)result.get("name")).collect(Collectors.toList());
    }

    public Set<String> getRooms() {
        if(!roomsLoaded)
            loadAllRooms();

        return roomIdCache.keySet();
    }

    // ----- Private -----

    private List<Map<String, Object>> loadAllRooms() {
        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 1000);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        results.stream().forEach((result) -> roomIdCache.put((String)result.get("name"), (Integer)result.get("id")));
        roomsLoaded = true;
        return results;
    }
}
