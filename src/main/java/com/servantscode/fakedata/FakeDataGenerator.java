package com.servantscode.fakedata;

import com.servantscode.fakedata.generator.*;

import java.io.IOException;
import java.time.LocalDate;

public class FakeDataGenerator extends DataImport {
    public static void main(String[] args) throws IOException {
//        doLogin();

//        FamilyGenerator.generate(100);
//        MinistryGenerator.generate(1000);
        DonationGenerator.generate(40);
        RoomGenerator.generate();
        EquipmentGenerator.generate();
        EventGenerator.generate();
        FormationGenerator.generate();
        new SacramentGenerator().generate();
        LocalDate startOfLastYear = LocalDate.now().withDayOfYear(1).minusYears(1);
        DonationGenerator.generate(40, startOfLastYear, startOfLastYear.plusYears(1).minusDays(1));
    }
}

