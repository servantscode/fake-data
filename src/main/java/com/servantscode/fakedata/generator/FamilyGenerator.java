package com.servantscode.fakedata.generator;

import com.servantscode.fakedata.client.PersonServiceClient;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.servantscode.fakedata.generator.RandomSelector.*;

public class FamilyGenerator {

    private static Map<Integer, Integer> childDistribution;
    private static List<String> streetTypes;
    private static List<String> emailDomains;

    static {
        childDistribution = new HashMap<>(10);
        childDistribution.put(0, 30);
        childDistribution.put(1, 15);
        childDistribution.put(2, 28);
        childDistribution.put(3, 17);
        childDistribution.put(4, 4);
        childDistribution.put(5, 2);
        childDistribution.put(6, 1);
        childDistribution.put(7, 1);
        childDistribution.put(8, 1);
        childDistribution.put(9, 1);

        streetTypes = Arrays.asList("Ln", "St", "Ave", "Dr", "Blvd");
        emailDomains = Arrays.asList("yahoo.com", "gmail.com", "hotmail.com", "LAST_NAME", "me.com");
    }

    private final PersonServiceClient personClient;

    public FamilyGenerator() {
        personClient = new PersonServiceClient();
    }

    public static void generate(int numberOfFamilies) throws IOException {
        for(int i=0; i<numberOfFamilies; i++)
            new FamilyGenerator().generateFamily();
    }

    private boolean headFound = false;
    private String surname;
    private Map<String, Object> familyData = new HashMap<>();

    private void generateFamily() throws IOException {
        List<String> streetNames = ListLoader.loadList("street-names.txt");
        Map<String,String> zipCodes = ListLoader.loadMap("zip-codes.txt");

        surname = getLastName();

        String street1 = String.format("%d %s %s", rand.nextInt(12000), select(streetNames), select(streetTypes));
        String zip = select(zipCodes.keySet());
        String city = zipCodes.get(zip);
        String state = "TX";
        String homePhone = randomPhoneNumber();

        Map<String, Object> addressData = new HashMap<>();
        addressData.put("street1", street1);
        addressData.put("city", city);
        addressData.put("state", state);
        addressData.put("zip", zip);

        familyData.put("surname", surname);
        familyData.put("address", addressData);
        familyData.put("homePhone", homePhone);
        if(rand.nextBoolean())
            familyData.put("envelopeNumber", rand.nextInt(10000));

        System.out.println(String.format("\nFamily: %s Address: %s, %s, %s %s", surname, street1, city, state, zip));

        int childCount = weightedSelect(childDistribution);
        generateFather(surname);
        generateMother(surname);
        for(int i=0; i<childCount; i++) {
            generateChild(surname);
        }
    }

    private String getLastName() throws IOException {
        List<String> lastNames = ListLoader.loadList("family-names.txt");
        return select(lastNames);
    }

    private void generateFather(String surname) throws IOException {
        int age = rand.nextInt(30) + 25;
        generatePerson(surname,true, age);
    }

    private void generateMother(String surname) throws IOException {
        int age = rand.nextInt(30) + 25;

        int lastNameModifier = rand.nextInt(100);
        if(lastNameModifier > 96) {
            surname = getLastName() + "-" + surname;
        } else if(lastNameModifier > 90) {
            surname = getLastName();
        }

        generatePerson(surname, false, age);
    }

    private void generateChild(String surname) throws IOException {
        int age = rand.nextInt(26);
        boolean male = rand.nextBoolean();
        generatePerson(surname, male, age);
    }

    private void generatePerson(String surname, boolean male, int age) throws IOException {
        String gender = male? "male": "female";
        List<String> names = ListLoader.loadList(gender + "-names.txt");

        String firstName = select(names);
        String name = firstName + " " + surname;

        String domain = select(emailDomains);
        String email = firstName + "@" + (domain.equals("LAST_NAME")? surname + ".com": domain);

        String phone = randomPhoneNumber();

        boolean head = !headFound;
        headFound = true;

        ZonedDateTime birthdate = randomDate(age);
        ZonedDateTime joined = (age == 0)? birthdate: randomDate(rand.nextInt(age));

        System.out.println(String.format("Name: %s email: %s phone:%s male?:%b headOfHousehold:%b birthdate:%tF joined:%tF", name, email, phone, male, head, birthdate, joined));

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("male", male);
        data.put("email", email);
        data.put("phoneNumber", phone);
        data.put("headOfHousehold", head);
        data.put("birthdate", birthdate);
        data.put("memberSince", joined);
        data.put("family", familyData);

        Map<String, Object> createdPersonData = personClient.createPerson(data);
        Map<String, Object> createdFamilyData = (Map<String, Object>) createdPersonData.get("family");
        familyData.put("id", createdFamilyData.get("id"));
    }

    private static ZonedDateTime randomDate(int age) {
        return ZonedDateTime.now().minusYears(age).withDayOfYear(rand.nextInt(365)+1);
    }
}
