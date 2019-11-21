package com.servantscode.fakedata.generator;

import org.servantscode.client.BaptismServiceClient;
import org.servantscode.client.ConfirmationServiceClient;
import org.servantscode.client.MarriageServiceClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static com.servantscode.fakedata.generator.RandomSelector.randomSelector;

public class SacramentGenerator {
    private final BaptismServiceClient baptismClient;
    private final ConfirmationServiceClient confirmationClient;
    private final MarriageServiceClient marriageServiceClient;
    private final PersonSelector personSelector;
    private final Map<String, Object> priest;

    public SacramentGenerator() {
        baptismClient = new BaptismServiceClient();
        confirmationClient = new ConfirmationServiceClient();
        marriageServiceClient = new MarriageServiceClient();
        personSelector = new PersonSelector();
        priest = generatePriest();
    }

    private Map<String, Object> generatePriest() {
        return PriestGenerator.generate(1).get(0);
    }


    public void generate() {

        int personCount = personSelector.personCount();
        for(int i=0; i<personCount; i++) {
            Map<String, Object> person = personSelector.getPerson(i);
            generatePersonalSacraments(person, personSelector.getFatherIdenty((Integer) person.get("id")), personSelector.getMotherIdenty((Integer) person.get("id")));
        }
    }

    public void generatePersonalSacraments(Map<String, Object> person, Map<String, Object> father, Map<String, Object> mother) {
        Map<String, Object> baptism = null;
        if(father == null && mother == null)
            return;

        LocalDate joinedDate =  LocalDate.parse((String)person.get("memberSince"), DateTimeFormatter.ISO_DATE);
        LocalDate birthdate = LocalDate.parse((String)person.get("birthdate"), DateTimeFormatter.ISO_DATE);

        if((boolean)person.get("baptized") && !joinedDate.isAfter(birthdate))
            baptism = generateBaptism(person, father, mother);

        if((boolean)person.get("confirmed") && !joinedDate.isAfter(birthdate.plusYears(12)))
            generateConfirmation(person, father, mother, baptism);
    }

    private Map<String, Object> generateBaptism(Map<String, Object> person, Map<String, Object> father, Map<String, Object> mother) {
        LocalDate birthdate = LocalDate.parse((String)person.get("birthdate"), DateTimeFormatter.ISO_DATE);
        Map<String, Object> baptism = new HashMap<>();
        baptism.put("person", getIdentity(person));
        baptism.put("father", getIdentity(father));
        baptism.put("mother", getIdentity(mother));
        baptism.put("baptismLocation", "St. George Catholic Parish");
        baptism.put("baptismDate", birthdate.plusDays(RandomSelector.nextInt(10, 90)));
        baptism.put("birthLocation", randomSelector().randomLocation());
        baptism.put("birthDate", birthdate);
        baptism.put("minister", getIdentity(priest));
        baptism.put("godfather", getIdentity(personSelector.randomAdultMan()));
        baptism.put("godmother", getIdentity(personSelector.randomAdultWoman()));
        return baptismClient.createBaptism(baptism);
    }

    private Map<String, Object> generateConfirmation(Map<String, Object> person, Map<String, Object> father, Map<String, Object> mother, Map<String, Object> baptism) {
        LocalDate birthdate = LocalDate.parse((String)person.get("birthdate"), DateTimeFormatter.ISO_DATE);
        Map<String, Object> confirmation = new HashMap<>();
        confirmation.put("person", getIdentity(person));
        confirmation.put("father", getIdentity(father));
        confirmation.put("mother", getIdentity(mother));
        if(baptism == null) {
            confirmation.put("baptismLocation", "St. George Catholic Parish");
            confirmation.put("baptismDate", birthdate.plusDays(RandomSelector.nextInt(10, 90)));
        } else {
            confirmation.put("baptismId", baptism.get("id"));
            confirmation.put("baptismLocation", baptism.get("baptismLocation"));
            confirmation.put("baptismDate", baptism.get("baptismDate"));
        }
        confirmation.put("sponsor", getIdentity(personSelector.randomAdult()));
        confirmation.put("confirmationLocation", "St. George Catholic Parish");
        confirmation.put("confirmationDate", birthdate.plusYears(12).withDayOfYear(RandomSelector.nextInt(1, 365)));
        confirmation.put("minister", getIdentity(priest));
        return confirmationClient.createConfirmation(confirmation);
    }

    public void generateMarriage(Map<String, Object> groom, Map<String, Object> bride) throws IOException {

    }

    private Map<String, Object> getIdentity(Map<String, Object> person) {
        if(person == null)
            return null;
        Map<String, Object> identity = new HashMap<>();
        identity.put("name", person.get("name"));
        identity.put("id", person.get("id"));
        return identity;
    }

}
