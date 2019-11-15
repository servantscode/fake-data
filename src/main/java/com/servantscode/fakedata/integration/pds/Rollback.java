package com.servantscode.fakedata.integration.pds;

import com.servantscode.fakedata.client.FamilyServiceClient;

public class Rollback {
    public static void main(String[] args) {
//        BaseServiceClient.setUrlPrefix("https://<parish>.servantscode.org");
//        BaseServiceClient.login("user", "password");

        FamilyServiceClient familyClient = new FamilyServiceClient();
        for(int i=39; i<1326; i++)
            familyClient.deleteFamilyId(i);
    }
}
