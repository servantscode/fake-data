package com.servantscode.fakedata;

import com.servantscode.fakedata.client.AbstractServiceClient;
import com.servantscode.fakedata.generator.*;

import java.io.IOException;
import java.text.Format;

public class FakeDataGenerator {
    public static void main(String[] args) throws IOException {
//        AbstractServiceClient.setUrlPrefix("https://demo.servantscode.org");
//        AbstractServiceClient.login("" , "");
//        FamilyGenerator.generate(100);
//        MinistryGenerator.generate(1000);
//        DonationGenerator.generate(40);
//        RoomGenerator.generate();
//        EquipmentGenerator.generate();
//        EventGenerator.generate();
//        FormationGenerator.generate();
        new SacramentGenerator().generate();
    }
}

