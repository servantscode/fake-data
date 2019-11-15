package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SessionServiceClient extends BaseServiceClient {

    public SessionServiceClient(int id) { super(String.format("/rest/program/%d/session", id)); }

    public Map<String, Object> createSession(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created program session: " + data.get("name"));
        else
            System.err.println("Failed to create program session. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }


    public List<Map<String, Object>> getPastSessions() {
        Map<String, Object> params = new HashMap<>(8);
        params.put("count", 0);

        Response response = get(params);
        Map<String, Object> resp = response.readEntity(new GenericType<Map<String, Object>>(){});

        List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
        return results.stream().filter((result) -> LocalDate.now().isAfter(ChronoLocalDate.from(ZonedDateTime.parse((CharSequence) result.get("startTime"), DateTimeFormatter.ISO_ZONED_DATE_TIME)))).collect(Collectors.toList());
    }
}
