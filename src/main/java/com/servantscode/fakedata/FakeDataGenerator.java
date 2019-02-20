package com.servantscode.fakedata;

import java.io.IOException;

public class FakeDataGenerator {
    public static void main(String[] args) throws IOException {
//        InitialLoginGenerator.generate();
        FamilyGenerator.generate(100);
        MinistryGenerator.generate(1000);
        DonationGenerator.generate(40);
        RoomGenerator.generate();
        EquipmentGenerator.generate();
        EventGenerator.generate();
    }
}
