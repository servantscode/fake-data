package com.servantscode.fakedata.generator;

import com.servantscode.fakedata.client.RoomServiceClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.servantscode.fakedata.generator.RandomSelector.rand;
import static com.servantscode.fakedata.generator.RandomSelector.select;

public class RoomGenerator {

    private static final RoomServiceClient roomClient = new RoomServiceClient();

    public static void generate() throws IOException {
        generateSanctuaries();
        generateMeetingRooms();
        generateClassRooms();
        generateKitchen();
        generateOffices();
    }

    private static void generateOffices() {
        createRoom("Pastor's Office", "OFFICE", 10);
        createRoom("Vicar's Office", "OFFICE", 3);
        createRoom("Deacon's Office", "OFFICE", 3);
        createRoom("Mike's Office", "OFFICE", 3);
    }

    private static void generateKitchen() {
        createRoom("Main Kitchen", "KITCHEN", 10);
        createRoom("Office Kitchen", "KITCHEN", 2);
    }

    private static void generateClassRooms() {
        for(int i=0; i<10; i++)
            createRoom("Room 10" + i, "CLASS", 25);

        for(int i=0; i<10; i++)
            createRoom("Room 20" + i, "CLASS", 30);
    }

    private static void generateMeetingRooms() throws IOException {
        List<String> names = ListLoader.loadList( "male-names.txt");

        for(int i=0; i<10; i++)
            createRoom("St. " + select(names), "MEETING", rand.nextInt(60));
    }

    private static void generateSanctuaries() {
        createRoom("Main Church", "SANCTUARY", rand.nextInt(20)*100);
        createRoom("Chapel", "SANCTUARY", rand.nextInt(20)*10);
    }

    private static void createRoom(String name, String type, int capacity) {
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", name);
        roomData.put("type", type);
        roomData.put("capacity", capacity);

        roomClient.createRoom(roomData);
    }
}
