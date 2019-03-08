package com.servantscode.fakedata.client;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;


public class PledgeServiceClient extends AbstractServiceClient {

//    public PledgeServiceClient() { super("http://donation-svc:8080/rest/pledge"); }
    public PledgeServiceClient() { super("/rest/pledge"); }

    public Map<String, Object> createDonation(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() == 200)
            System.out.println("Created pledge for: " + data.get("familyId"));
        else
            System.err.println("Failed to create pledge. Status: " + response.getStatus());

        Map<String, Object> pledge = response.readEntity(new GenericType<Map<String, Object>>(){});
        fixFloat(pledge, "pledgeAmount");
        return pledge;
    }

    private void fixFloat(Map<String, Object> pledge, String key) {
        double dbl = (double) pledge.get(key);
        float flt = (float) dbl;
        pledge.put(key, flt);
    }
}
