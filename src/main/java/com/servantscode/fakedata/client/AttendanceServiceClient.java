package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class AttendanceServiceClient extends BaseServiceClient {

    public AttendanceServiceClient(int programId) { super(String.format("/rest/program/%d", programId)); }

    public Map<String, Object> recordAttendance(Map<String, Object> data, int sectionId) {
        Response response = put(String.format("section/%d/attendance", sectionId), data);

        if(response.getStatus() == 200)
            System.out.println("Attendance recorded");
        else
            System.err.println("Failed to record attendance. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
