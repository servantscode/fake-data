package com.servantscode.fakedata.client;

import javax.ws.rs.core.Response;
import java.util.Map;

public class EnrollmentServiceClient extends AbstractServiceClient {

    //public EnrollmentServiceClient() { super("http://ministry-svc:8080/rest/enrollment"); }
    public EnrollmentServiceClient() { super("http://localhost/rest/enrollment"); }

    public void createEnrollment(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created Enrollment: " + data.get("personId"));
        else
            System.err.println("Failed to create enrollment. Status: " + response.getStatus());
    }
}
