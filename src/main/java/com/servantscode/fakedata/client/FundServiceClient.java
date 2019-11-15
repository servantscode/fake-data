package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class FundServiceClient extends BaseServiceClient {

    //public DonationServiceClient() { super("http://donation-svc:8080/rest/donation"); }
    public FundServiceClient() { super("/rest/fund"); }

    public Map<String, Object> createFund(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created fund: " + data.get("name"));
        else
            System.err.println("Failed to create fund. Status: " + response.getStatus());

        return response.readEntity(new GenericType<Map<String, Object>>(){});
    }
}
