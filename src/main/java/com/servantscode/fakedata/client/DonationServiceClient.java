package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class DonationServiceClient extends AbstractServiceClient {

    //public DonationServiceClient() { super("http://donation-svc:8080/rest/donation"); }
    public DonationServiceClient() { super("http://localhost/rest/donation"); }

    public Map<String, Object> createDonation(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created donation for: " + data.get("familyId"));
        else
            System.err.println("Failed to create donation. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
