package com.servantscode.fakedata.generator;

import com.servantscode.fakedata.client.PersonServiceClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static com.servantscode.fakedata.generator.RandomSelector.*;
import static java.util.Arrays.asList;

public class PriestGenerator {

    public static final List<String> ethnicities = asList("AFRICAN_AMERICAN", "ASIAN", "CAUCASIAN", "HINDU", "HISPANIC", "LATINO", "VIETNAMESE", "OTHER");
    public static final List<String> languages = asList("ENGLISH", "SPANISH", "VIETNAMESE", "OTHER");

    private static List<String> streetTypes;
    private static List<String> emailDomains;

    static {
        streetTypes = Arrays.asList("Ln", "St", "Ave", "Dr", "Blvd");
        emailDomains = Arrays.asList("yahoo.com", "gmail.com", "hotmail.com", "LAST_NAME", "me.com");
    }

    private final PersonServiceClient personClient;

    public PriestGenerator() {
        personClient = new PersonServiceClient();
    }

    public static List<Map<String, Object>> generate(int numberOfPriests) {
        List<Map<String, Object>> priests = new LinkedList<>();
        for(int i=0; i<numberOfPriests; i++)
            priests.add(new PriestGenerator().generateFamily());
        return priests;
    }

    private String surname;
    private Map<String, Object> familyData = new HashMap<>();

    private LocalDate familyJoinedDate = null;

    private Map<String, Object> generateFamily() {
        surname = getLastName();

        String street1 = String.format("%d %s %s", rand.nextInt(12000), select(randomSelector().streetNames), select(streetTypes));
        String zip = select(randomSelector().zipCodes.keySet());
        String city = randomSelector().zipCodes.get(zip);
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
        if (rand.nextBoolean())
            familyData.put("envelopeNumber", rand.nextInt(10000));

        System.out.println(String.format("\nFamily: %s Address: %s, %s, %s %s", surname, street1, city, state, zip));

        return generatePriest(surname);
    }

    private String getLastName() {
        List<String> lastNames = ListLoader.loadList("family-names.txt");
        return select(lastNames);
    }

    private Map<String, Object> generatePriest(String surname) {
        int age = RandomSelector.nextInt(25, 75);
        String gender = "male";
        List<String> names = ListLoader.loadList(gender + "-names.txt");

        String firstName = select(names);
        String name = firstName + " " + surname;

        String domain = select(emailDomains);
        String email = firstName + "@" + (domain.equals("LAST_NAME")? surname + ".com": domain);

        LocalDate birthdate = randomDate(age);
        LocalDate joined = randomDate(rand.nextInt(age));

        System.out.println(String.format("Name: %s email: %s birthdate:%tF joined:%tF", name, email, birthdate, joined));

        String religion = "CATHOLIC";
        boolean baptized = true;
        boolean confession = true;
        boolean communion = true;
        boolean confirmed = true;
        boolean holyOrders = true;

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("male", true);
        data.put("email", email);
        data.put("headOfHousehold", true);
        data.put("phoneNumbers", randomPhoneNumbers());
        data.put("birthdate", birthdate);
        data.put("parishioner", true);
        data.put("memberSince", joined);
        data.put("family", familyData);
        data.put("salutation", "Fr.");
        data.put("baptized", baptized);
        data.put("confession", confession);
        data.put("communion", communion);
        data.put("confirmed", confirmed);
        data.put("holyOrders", holyOrders);
        data.put("maritalStatus", "SINGLE");
        data.put("ethnicity", selectEnum(ethnicities, "CAUCASIAN", .20f));
        data.put("primaryLanguage", selectEnum(languages, "ENGLISH", .5f));
        data.put("religion", religion);
        Map<String, Object> createdPersonData = personClient.createPerson(data);
        return createdPersonData;
    }

    private static String selectEnum(List<String> options, String defaultValue, float defaultPercentage) {
        if(RandomSelector.rand.nextFloat() < defaultPercentage) {
            return defaultValue;
        }
        return RandomSelector.select(options);
    }

    private static LocalDate randomDate(int age) {
        return LocalDate.now().minusYears(age).withDayOfYear(rand.nextInt(365)+1);
    }
}
