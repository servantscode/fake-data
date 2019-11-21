package com.servantscode.fakedata.generator;

import org.servantscode.client.EquipmentServiceClient;

import java.util.HashMap;
import java.util.Map;

public class EquipmentGenerator {
    private static final EquipmentServiceClient equipClient = new EquipmentServiceClient();

    public static void generate() {
        generateTVs();
        generateProjectors(3);
        generateCoffeePots(5);
        generatePopcornMaker();
        generateSnowconeMachine();
    }

    private static void generateSnowconeMachine() {
        generateEquipment("Snow cone machine", "Kona", "Good on a hot day in Texas");
    }

    private static void generatePopcornMaker() {
        generateEquipment("Popcorn Maker", "Orville Reddenbacher", "Pop, pop, pop");
    }

    private static void generateCoffeePots(int count) {
        for(int i=0;i<count; i++)
            generateEquipment("Coffee Pot " + (i+1), "Bunn", "Makes Coffee");
    }

    private static void generateProjectors(int count) {
        for(int i=0;i<count; i++)
            generateEquipment("Projector " + (i+1), "Panasonic", "Portable Projector");
    }

    private static void generateTVs() {
        generateEquipment("Big TV", "Sony", "The TV on wheels");
        generateEquipment("Little TV", "Sony", "The TV you carry");
    }

    private static void generateEquipment(String name, String manufacturer, String description) {
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", name);
        roomData.put("manufacturer", manufacturer);
        roomData.put("description", description);

        equipClient.createEquipment(roomData);
    }
}
