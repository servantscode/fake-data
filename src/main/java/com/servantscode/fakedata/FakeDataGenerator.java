package com.servantscode.fakedata;

import com.servantscode.fakedata.generator.*;

import java.io.IOException;

public class FakeDataGenerator {
    public static void main(String[] args) throws IOException {
//        BaseServiceClient.setUrlPrefix("https://demo.servantscode.org");
//        BaseServiceClient.login("" , "");
        FamilyGenerator.generate(100);
        MinistryGenerator.generate(1000);
        DonationGenerator.generate(40);
        RoomGenerator.generate();
        EquipmentGenerator.generate();
        EventGenerator.generate();
        FormationGenerator.generate();
        new SacramentGenerator().generate();
    }
}

