package com.servantscode.fakedata.integration;

import org.servantscode.client.BaseServiceClient;
import org.servantscode.client.PersonServiceClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.servantscode.commons.StringUtils.isSet;

public class PersonImport {
    public static void main(String[] args) throws IOException {
        String filePath = "c:\\Users\\gleit\\stgabriel\\people.csv";
        File importFile = new File(filePath);

        BaseServiceClient.setUrlPrefix("https://stgabriel.servantscode.org");
        BaseServiceClient.login("greg@servantscode.org", "S3rv@nt1HasTh1s");
        new PersonImport().processFile(importFile, false);
    }

    private PersonServiceClient client;

    public PersonImport() {
        client = new PersonServiceClient();
    }

    private void processFile(File importFile, boolean hasHeaders) {
        int lineNumber = 0;
        String line = null;
        try {
            BufferedReader fileLines = new BufferedReader(new FileReader(importFile));

            String headers = null;
            if(hasHeaders) {
                headers = fileLines.readLine();
                System.out.println("Found header line: " + headers);
            }

            List<String> badLines = new LinkedList<>();

            while((line = fileLines.readLine()) != null) {
                String[] parsedLine = CSVParser.parseCsvLine(line);

                //Horray for fixed field structures!
                String name = parsedLine[0];
                String email = parsedLine[1];
                String male = parsedLine[2];
                String phone = parsedLine[3];
                String street = parsedLine[4];
                String city = parsedLine[5];
                String state = parsedLine[6];
                String zip = parsedLine[7];

                HashMap<String, Object> person = new HashMap<>(16);
                person.put("name", name);
                person.put("email", email);
                person.put("phoneNumber", phone);
                person.put("male", Boolean.parseBoolean(male));

                HashMap<String, Object> family = new HashMap<>();
                family.put("surname", name.substring(name.lastIndexOf(" ")));

                HashMap<String, Object> address = new HashMap<>();
                address.put("street1", street);
                address.put("city", city);
                address.put("state", state);
                address.put("zip", zip);
                family.put("address", address);

                person.put("family", family);

                client.createPerson(person);

                lineNumber++;
            }

            if(badLines.isEmpty())
                System.out.println(String.format("Processed %d lines. 0 failures", lineNumber));
            else {
                System.err.println(String.format("Processed %d lines. %d failures", lineNumber, badLines.size()));
                if(isSet(headers))
                    System.out.println(headers);
                badLines.forEach(System.err::println);
            }

        } catch (Throwable e) {
            System.err.println("File import failed!");
            e.printStackTrace();
            System.err.println("Failing line: " + line);
        }

        System.out.println(String.format("Successfully imported %d lines.", lineNumber));
    }
}
