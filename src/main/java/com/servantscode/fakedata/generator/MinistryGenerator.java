package com.servantscode.fakedata.generator;

import com.servantscode.fakedata.client.EnrollmentServiceClient;
import com.servantscode.fakedata.client.MinistryRoleServiceClient;
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

        Map<String, Integer> roleIds = generateDefaultRoles(ministryId);

        ArrayList<Integer> selectedPeople = new ArrayList<>(enrollment);
        for(int i=0; i<enrollment; i++) {
            int personId;
            do {
                personId = rand.nextInt(peopleCount) + 1;
            } while(selectedPeople.contains(personId));

            selectedPeople.add(personId);

            String role = selectRole(selectedPeople.size());
            int roleId = roleIds.get(role);

            Map<String, Object> enrollmentData = new HashMap<>();
            enrollmentData.put("personId", personId);
            enrollmentData.put("ministryId", ministryId);
            enrollmentData.put("roleId", roleId);

            enrollmentClient.createEnrollment(enrollmentData);
        }
    }

    private static Map<String, Integer> generateDefaultRoles(int ministryId) {
        Map<String, Integer> roleIdMap = new HashMap<>(8);
        MinistryRoleServiceClient roleClient = new MinistryRoleServiceClient(ministryId);
        Map<String, Object> president = roleClient.createMinistryRole(generateRole(ministryId, "president", true, true));
        roleIdMap.put("president", (Integer)president.get("id"));
        Map<String, Object> secretary = roleClient.createMinistryRole(generateRole(ministryId, "secretary", false, true));
        roleIdMap.put("secretary", (Integer)secretary.get("id"));
        Map<String, Object> member = roleClient.createMinistryRole(generateRole(ministryId, "member", false, false));
        roleIdMap.put("member", (Integer)member.get("id"));
        return roleIdMap;
    }

    private static Map<String, Object> generateRole(int ministryId, String role, boolean contact, boolean leader) {
        Map<String, Object> roleData = new HashMap<>();
        roleData.put("name", role);
        roleData.put("ministryId", ministryId);
        roleData.put("contact", contact);
        roleData.put("leader", leader);
        return roleData;
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
