package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;


public class BatchDonationServiceClient extends AbstractServiceClient {

    public BatchDonationServiceClient() { super("/rest/donation/batch"); }

    public void createDonations(List<Map<String, Object>> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created " + data.size() + " donations.");
        else
            System.err.println("Failed to create donations. Status: " + response.getStatus());
    }
}
