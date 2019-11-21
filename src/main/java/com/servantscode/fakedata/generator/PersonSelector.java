package com.servantscode.fakedata.generator;

import org.servantscode.client.PersonServiceClient;
import org.servantscode.client.RelationshipServiceClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PersonSelector {
    public PersonServiceClient personClient;
    public RelationshipServiceClient relationshipClient;

    private static String adultQuery = "birthdate:[1954-01-01 TO 1994-01-01]"; //25-65
    private static String adultManQuery = "birthdate:[1954-01-01 TO 1994-01-01] male:true"; //25-65
    private static String adultWomanQuery = "birthdate:[1954-01-01 TO 1994-01-01] male:false"; //25-65
    private int adultCount = 0;
    private int manCount = 0;
    private int womanCount = 0;

    public PersonSelector() {
        personClient = new PersonServiceClient();
        relationshipClient = new RelationshipServiceClient();
    }

    public int personCount() {
        return personClient.getPeopleCount();
    }

    public Map<String, Object> getPerson(int index) {
        return getPerson(index, null);
    }

    public Map<String, Object> getPerson(int index, String query) {
        return personClient.getPerson(index, query);
    }

    public Map<String, Object> randomAdult() {
        if(adultCount == 0)
            adultCount = personClient.getPeopleCount(adultQuery);

        return getPerson(RandomSelector.nextInt(0, adultCount), adultQuery);
    }

    public Map<String, Object> randomAdultMan() {
        if(manCount == 0)
            manCount = personClient.getPeopleCount(adultManQuery);

        return getPerson(RandomSelector.nextInt(0, manCount), adultManQuery);
    }

    public Map<String, Object> randomAdultWoman() {
        if(womanCount == 0)
            womanCount = personClient.getPeopleCount(adultWomanQuery);

        return getPerson(RandomSelector.nextInt(0, womanCount), adultWomanQuery);
    }

    public Map<String, Object> getFatherIdenty(int id) {
        List<Map<String, Object>> relationships = relationshipClient.getRelationships(id);
        Optional<Map<String, Object>> father = relationships.stream().filter(r -> r.get("relationship").equals("FATHER")).map(r -> {
               Map<String, Object> res = new HashMap<>();
               res.put("id", r.get("otherId"));
               res.put("name", r.get("otherName"));
               return res;
            }).findFirst();
        return father.orElse(null);
    }

    public Map<String, Object> getMotherIdenty(int id) {
        List<Map<String, Object>> relationships = relationshipClient.getRelationships(id);
        Optional<Map<String, Object>> father = relationships.stream().filter(r -> r.get("relationship").equals("MOTHER")).map(r -> {
            Map<String, Object> res = new HashMap<>();
            res.put("id", r.get("otherId"));
            res.put("name", r.get("otherName"));
            return res;
        }).findFirst();
        return father.orElse(null);
    }
}
