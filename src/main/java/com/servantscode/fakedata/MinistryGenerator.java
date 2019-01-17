package com.servantscode.fakedata;

import com.servantscode.fakedata.client.EnrollmentServiceClient;
import com.servantscode.fakedata.client.MinistryServiceClient;
import com.servantscode.fakedata.client.PersonServiceClient;

import java.io.IOException;
import java.util.*;

public class MinistryGenerator {
    private static Random rand = new Random();

    private static PersonServiceClient personClient = new PersonServiceClient();
    private static MinistryServiceClient ministryClient = new MinistryServiceClient();
    private static EnrollmentServiceClient enrollmentClient = new EnrollmentServiceClient();

    private static int peopleCount;

    public static void generate(int registrations) throws IOException {
        List<String> ministries = ListLoader.loadList("ministries.txt");

        int allocation = registrations/ministries.size();

        peopleCount = personClient.getPeopleCount();

        for(String ministry: ministries) {
            int enrollment = allocation/2 + rand.nextInt(allocation);
            generateMinistry(ministry, enrollment);
        }
    }

    private static void generateMinistry(String name, int enrollment) {
        String description = "Serve the church";

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", description);

        System.out.println(String.format("Ministry:%s Description:%s enrollment:%d", name, description, enrollment));
        Map<String, Object> returnedMinistry = ministryClient.createMinistry(data);
        int ministryId = (Integer) returnedMinistry.get("id");

        ArrayList<Integer> selectedPeople = new ArrayList<>(enrollment);
        for(int i=0; i<enrollment; i++) {
            int personId;
            do {
                personId = rand.nextInt(peopleCount) + 1;
            } while(selectedPeople.contains(personId));

            selectedPeople.add(personId);

            String role = selectRole(selectedPeople.size());

            Map<String, Object> enrollmentData = new HashMap<>();
            enrollmentData.put("personId", personId);
            enrollmentData.put("ministryId", ministryId);
            enrollmentData.put("role", role);

            enrollmentClient.createEnrollment(enrollmentData);
        }
    }

    private static String selectRole(int roleSelector) {
        switch(roleSelector) {
            case 1:
                return "president";
            case 2:
                return "secretary";
            default:
                return "member";
        }
    }
}
