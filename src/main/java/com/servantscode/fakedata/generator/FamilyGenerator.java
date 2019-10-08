package com.servantscode.fakedata.generator;

import com.servantscode.fakedata.client.PersonServiceClient;
import com.servantscode.fakedata.client.RelationshipServiceClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static com.servantscode.fakedata.generator.RandomSelector.*;
import static java.util.Arrays.asList;
import static org.servantscode.commons.StringUtils.isSet;

public class FamilyGenerator {

    public static final List<String> marriedStatuses = asList("MARRIED_IN_CHURCH", "MARRIED_OUTSIDE_CHURCH", "MARRIED");
    public static final List<String> unmarriedStatuses = asList("SINGLE", "SEPARATED", "DIVORCED", "ANNULLED", "WIDOWED");
    public static final List<String> ethnicities = asList("AFRICAN_AMERICAN", "ASIAN", "CAUCASIAN", "HINDU", "HISPANIC", "LATINO", "VIETNAMESE", "OTHER");
    public static final List<String> languages = asList("ENGLISH", "SPANISH", "VIETNAMESE", "OTHER");
//    public List<String> SpecialNeeds { ARTHRITIS, ASTHMA, BLIND, CANCER, DEAF, EMPHYSEMA, HEARING_IMPAIRED, HEART_DISEASE, IMPAIRED_MOBILITY, MANIC_DEPRESSION, MULTIPLE_SCLEROSIS, VISUALLY_IMPAIRED, WHEEL_CHAIR };

    public static final List<String> religions = asList("ASSEMBLY_OF_GOD", "BAPTIST", "BUDDHIST", "CATHOLIC", "CHRISTIAN", "CHURCH_OF_GOD", "CONGREGATIONAL", "EASTERN_RITE_ORTHODOX", "EPISCOPALIAN", "EVANGELICAL", "GREEK_ORTHODOX", "HINDU", "JEWISH", "LDS", "LUTHERAN",
        "METHODIST", "MORMAN", "MUSLIM", "NAZARENE", "NONDENOMINATIONAL", "NONE", "OTHER", "PENTECOSTAL", "PRESBYTERIAN", "PROTESTANT", "RUSSIAN_ORTHODOX", "UNDECIDED");

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
    private final RelationshipServiceClient relationshipClient;

    public FamilyGenerator() {
        personClient = new PersonServiceClient();
        relationshipClient = new RelationshipServiceClient();
    }

    public static void generate(int numberOfFamilies) throws IOException {
        for(int i=0; i<numberOfFamilies; i++)
            new FamilyGenerator().generateFamily();
    }

    private boolean headFound = false;
    private String surname;
    private boolean singleParent;
    private String marriageType;
    private int youngestParent = Integer.MAX_VALUE;
    private Map<String, Object> familyData = new HashMap<>();

    private int fatherId = 0;
    private int motherId = 0;
    private List<Integer> childIds = new LinkedList<>();

    private void generateFamily() throws IOException {
        List<String> streetNames = ListLoader.loadList("street-names.txt");
        Map<String, String> zipCodes = ListLoader.loadMap("zip-codes.txt");

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
        if (rand.nextBoolean())
            familyData.put("envelopeNumber", rand.nextInt(10000));

        System.out.println(String.format("\nFamily: %s Address: %s, %s, %s %s", surname, street1, city, state, zip));

        generateFather(surname);
        generateMother(surname);
        if (youngestParent < 55) {
            int childCount = weightedSelect(childDistribution);
            for (int i = 0; i < childCount; i++) {
                generateChild(surname);
            }
        }

        generateRelationships();
    }


    private String getLastName() throws IOException {
        List<String> lastNames = ListLoader.loadList("family-names.txt");
        return select(lastNames);
    }

    private void generateFather(String surname) throws IOException {
        int age = RandomSelector.nextInt(25, 90);

        if(nextInt(0,100) > 70) {
            singleParent = true;
            if(nextInt(0,100) < 50)
                return;
        }

        youngestParent = age;
        fatherId = generatePerson(surname,true, age, "Mr.", true);
    }

    private void generateMother(String surname) throws IOException {
        if(headFound && singleParent)
            return;

        int age = singleParent? RandomSelector.nextInt(25, 55): RandomSelector.nextInt(youngestParent-10, youngestParent+10);
        youngestParent = Math.min(youngestParent, age);

        int lastNameModifier = rand.nextInt(100);
        if(lastNameModifier > 96) {
            surname = getLastName() + "-" + surname;
        } else if(lastNameModifier > 90) {
            surname = getLastName();
        }

        motherId = generatePerson(surname, false, age, "Mrs.", true);
    }

    private void generateChild(String surname) throws IOException {
        int age = RandomSelector.nextInt(Math.max(youngestParent-40, 0), youngestParent-15);
        boolean male = rand.nextBoolean();
        String salutation = male? "Mr.": "Ms.";
        childIds.add(generatePerson(surname, male, age, salutation, false));
    }

    private int generatePerson(String surname, boolean male, int age, String salutation, boolean parent) throws IOException {
        String gender = male? "male": "female";
        List<String> names = ListLoader.loadList(gender + "-names.txt");

        String firstName = select(names);
        String name = firstName + " " + surname;

        String domain = select(emailDomains);
        String email = firstName + "@" + (domain.equals("LAST_NAME")? surname + ".com": domain);

        boolean head = !headFound;
        headFound = true;

        LocalDate birthdate = randomDate(age);
        LocalDate joined = (age == 0)? birthdate: randomDate(rand.nextInt(age));

        System.out.println(String.format("Name: %s email: %s male?:%b headOfHousehold:%b birthdate:%tF joined:%tF", name, email, male, head, birthdate, joined));

        String religion = selectEnum(religions, "CATHOLIC", .75f);
        boolean baptized = religion.equals("CATHOLIC")? byPercentage(.95f): byPercentage(.30f);
        boolean confession = baptized && age >= 7 && byPercentage(.75f);
        boolean communion = confession && byPercentage(.95f);
        boolean confirmed = communion && age >= 12 && byPercentage(.60f);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("male", male);
        data.put("email", email);
        data.put("headOfHousehold", head);
        data.put("phoneNumbers", randomPhoneNumbers());
        data.put("birthdate", birthdate);
        data.put("parishioner", true);
        data.put("memberSince", joined);
        data.put("family", familyData);
        data.put("salutation", salutation);
        data.put("baptized", baptized);
        data.put("confession", confession);
        data.put("communion", communion);
        data.put("confirmed", confirmed);
        data.put("maritalStatus", selectMarriageStatus(parent));
        data.put("ethnicity", selectEnum(ethnicities, "CAUCASIAN", .20f));
        data.put("primaryLanguage", selectEnum(languages, "ENGLISH", .5f));
        data.put("religion", religion);
        if(parent && !singleParent && !male && !surname.equals(familyData.get("surname")))
            data.put("maidenName", getLastName());

        Map<String, Object> createdPersonData = personClient.createPerson(data);
        Map<String, Object> createdFamilyData = (Map<String, Object>) createdPersonData.get("family");
        familyData.put("id", createdFamilyData.get("id"));
        return (int)createdPersonData.get("id");
    }

    private void generateRelationships() {
        List<Map<String, Object>> relationships = new LinkedList<>();
        if(fatherId > 0) {
            if(motherId > 0)
                relationships.add(generateRelationship(fatherId, motherId, "SPOUSE"));
            for(int child: childIds)
                relationships.add(generateRelationship(fatherId, child, "CHILD"));
        }

        if(motherId > 0) {
            for (int child : childIds)
                relationships.add(generateRelationship(motherId, child, "CHILD"));
        }

        int i = 0;
        for(int child1: childIds) {
            for (int child2 : childIds.subList(i + 1, childIds.size()))
                relationships.add(generateRelationship(child1, child2, "SIBLING"));
        }

        if(relationships.size() > 0)
            relationshipClient.createRelationships(relationships, true);
    }

    private Map<String, Object> generateRelationship(int subject, int other, String relationship) {
        Map<String, Object> r = new HashMap<>(8);
        r.put("personId", subject);
        r.put("otherId", other);
        r.put("relationship", relationship);
        if(relationship.equals("CHILD"))
            r.put("guardian", true);
        if(relationship.equals("CHILD") || relationship.equals("SPOUSE"))
            r.put("contactPreference", 1);
        return r;
    }

    private String selectMarriageStatus(boolean parent) {
        if(!parent)
            return "SINGLE";

        if(singleParent)
            return selectEnum(unmarriedStatuses, "SINGLE", .30f);

        if(!isSet(marriageType))
            marriageType = selectEnum(marriedStatuses, "MARRIED_IN_CHURCH", .55f);

        return marriageType;
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
