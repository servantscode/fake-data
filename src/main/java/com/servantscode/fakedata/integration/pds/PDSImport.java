package com.servantscode.fakedata.integration.pds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servantscode.fakedata.integration.CSVData;
import com.servantscode.fakedata.integration.CSVParser;
import org.servantscode.client.*;
import org.servantscode.commons.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class PDSImport {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String DEFAULT_SYSTEM = "https://demo.servantscode.org";
    public static final String DEFAULT_USER = "greg@servantscode.org";

    public static void main(String[] args) throws Exception {

        String familyFilePath = "C:\\Users\\gleit\\Desktop\\Parishes\\St. Mary\\family-active2.csv";
        String familyFilePath2 = "C:\\Users\\gleit\\Desktop\\Parishes\\St. Mary\\family-inactive.csv";
        String peopleFilePath = "C:\\Users\\gleit\\Desktop\\Parishes\\St. Mary\\sc-export.csv";
        String donationFilePath = "C:\\Users\\gleit\\Desktop\\Parishes\\St. Mary\\donation-history-all.csv";

        File familyFile = new File(familyFilePath);
        File familyFile2 = new File(familyFilePath2);
        File peopleFile = new File(peopleFilePath);
        File donationFile = new File(donationFilePath);

        boolean dryRun = false;

        doLogin();
        new PDSImport().processFiles(asList(familyFile, familyFile2), asList(peopleFile), asList(donationFile), dryRun);
    }

    private static void doLogin() throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Which system? [" + DEFAULT_SYSTEM + "]");
        String system = input.readLine();
        if(isEmpty(system))
            system = DEFAULT_SYSTEM;
        if(!system.contains("//"))
            system = "https://" + system;
        if(!system.contains("."))
            system = system + ".servantscode.org";

        System.out.println(String.format("Connecting to %s.", system));

        ApiClientFactory apiFactory = ApiClientFactory.instance();
        apiFactory.setExternalPrefix(system);

        System.out.println("User? [" + DEFAULT_USER + "]");
        String user = input.readLine();
        if(isEmpty(user))
            user = DEFAULT_USER;

        System.out.println("Password? []");
        String password = input.readLine();
        if(isEmpty(user))
            throw new RuntimeException("No password supplied.");

        BaseServiceClient.login(user, password);
    }

    HashMap<String, Map<String, Object>> knownFamilies = new HashMap<>(1024);
    HashMap<String, Integer> knownFunds = new HashMap<>(1024);

    PersonServiceClient personClient;
    FamilyServiceClient familyClient;
    RelationshipServiceClient relationshipClient;
    BatchDonationServiceClient donationClient;
    FundServiceClient fundClient;
    NoteServiceClient noteClient;

    private static final Pattern CITY_STATE_ZIP_PARSER = Pattern.compile("(\\w+(\\s)?)+,?\\s+(\\w{2})\\.?\\s+(\\d{5})(-\\d{4}?)?");

    public PDSImport() {
        personClient = new PersonServiceClient();
        familyClient = new FamilyServiceClient();
        relationshipClient = new RelationshipServiceClient();
        donationClient = new BatchDonationServiceClient();
        fundClient = new FundServiceClient();
        noteClient = new NoteServiceClient();
    }

    private void processFiles(List<File> familyInput, List<File> peopleInput, List<File> donationInput, boolean dryRun) throws Exception {
        CSVData personData = new CSVParser().readFiles(peopleInput);
        printCSVReport(personData);

        CSVData familyData = new CSVParser().readFiles(familyInput);
        printCSVReport(familyData);

        CSVData donationData = new CSVParser().readFiles(donationInput);
        printCSVReport(donationData);

        checkDuplicates(familyData, "Fam Last Name");

        createFamilies(familyData);
        generateTemplatePeople(familyData, personData);
        createPeople(personData);

//        storeFamilies(dryRun);
//        applyNotes(familyData, personData, dryRun);
//        updateRelationships(dryRun);
        createFunds(donationData, dryRun);
        importDonations(donationData, dryRun);
    }

    private void checkDuplicates(CSVData data, String field) {
        HashMap<String, AtomicInteger> counts = new HashMap<>(data.rowData.size());
        data.rowData.forEach(r -> counts.computeIfAbsent(r.get(field), e -> new AtomicInteger(0)).incrementAndGet());
        for(Map.Entry<String, AtomicInteger> nameCount: counts.entrySet()) {
            if(nameCount.getValue().get() > 1)
                System.out.println(String.format("Found duplicate %s: %s[%d]", field, nameCount.getKey(), nameCount.getValue().get()));
        }
    }

    private void printCSVReport(CSVData data) {
        System.out.println(String.format("Processed csv file. Found %d records.", data.rowData.size()));
        System.out.println(String.format("Found %d fields in records:", data.fields.size()));
        for(String field: data.fields) {
            List<HashMap<String, String>> samples = data.rowData.stream().filter(r -> isSet(r.get(field))).limit(5).collect(toList());
            System.out.println(String.format("\t%s: %d", field, data.fieldCounts.get(field).get()));
            if(!samples.isEmpty())
                System.out.println(String.format("\t\tSample values: %s", samples.stream().map(s -> s.get(field)).collect(Collectors.joining(", "))));
        }
    }

    private void createFamilies(CSVData familyData) {
        for(HashMap<String, String> row: familyData.rowData) {
            HashMap<String, Object> family = new HashMap<>(32);

            String surname = row.get("Fam Last Name");

            String numString = row.get("Fam ID/Env Number");
            int envelopeNumber = isSet(numString)? Integer.parseInt(numString): 0;

            add(family, "surname", surname);
            add(family, "envelopeNumber", envelopeNumber);
            add(family, "address", processAddress(getMultiValue(row, "Fam Address Block", "Fam Street Address Block")));
            add(family, "inactive", mapBoolean(row.get("Fam Inactive"), "YES"));
            add(family, "inactiveSince", mapDate(row.get("Fam Inactive Date")));

            int id = familyClient.getFamilyId(surname, envelopeNumber);
            if(id > 0)
                add(family, "id", id);

            String familyUID = row.get("Fam Unique ID");
            knownFamilies.put(familyUID, family);
        }

        System.out.println("Created " + knownFamilies.size() + " families.");
    }


    private void storeFamilies(boolean dryRun) throws JsonProcessingException {
        int familiesImported = 0;
        for(Map.Entry<String, Map<String, Object>> familyData: knownFamilies.entrySet()) {
            familiesImported++;
            Map<String, Object> createdFamily;
            if(dryRun) {
                System.out.println("Processed line: " + OBJECT_MAPPER.writeValueAsString(familyData.getValue()));
                createdFamily = familyData.getValue();
                createdFamily.put("id", familiesImported);
            } else {
                createdFamily = familyClient.createOrUpdateFamily(familyData.getValue());
                familyData.setValue(createdFamily);

            }
        }
    }

    private void applyNotes(CSVData familyData, CSVData personData, boolean dryRun) {
        for(HashMap<String, String> row: familyData.rowData) {
            String familyUID = row.get("Fam Unique ID");
            Map<String, Object> family = knownFamilies.get(familyUID);
            if (!dryRun) {
                storeNotes("family", family, row.get("Fam General Remarks"), false);
                storeNotes("family", family, row.get("Fam Confidential Remarks"), true);
            }
        }

        for(HashMap<String, String> row: personData.rowData) {
            String familyUID = row.get("Mem Fam Unique ID");
            String name = readName(row);
            if(isEmpty(familyUID)) {
                if(isSet(row.get("Mem General Remarks")) || isSet(row.get("Mem Confidential Remarks")))
                    throw new IllegalStateException("Remarks exist on a person row without a family id: " + name);
                else
                    continue;
            }

            Map<String, Object> family = knownFamilies.get(familyUID);
            Map<String, Object> person = getPersonFromFamily(name, family);
            if (!dryRun) {
                storeNotes("person", person, row.get("Mem General Remarks"), false);
                storeNotes("person", person, row.get("Mem Confidential Remarks"), true);
            }
        }
    }

    private Map<String, Object> getPersonFromFamily(String name, Map<String, Object> family) {
        List<Map<String, Object>> members = (List<Map<String, Object>>)family.get("members");
        if(members == null)
            throw new IllegalStateException("Couldn't find person in family: " + name);

        Optional<Map<String, Object>> foundPerson = members.stream().filter(m -> name.equals(m.get("name"))).findFirst();
        if(!foundPerson.isPresent())
            throw new IllegalStateException("Couldn't find person in family: " + name);

        return foundPerson.get();
    }

    private void storeNotes(String type, Map<String, Object> target, String remarks, boolean isPrivate) {
        if(isEmpty(remarks))
            return;

        Map<String, Object> note = new HashMap<>(8);
        note.put("resourceType", type);
        note.put("resourceId", target.get("id"));
        note.put("private", isPrivate);

        String[] notes = remarks.split(",,,");
        Arrays.stream(notes).forEach(n -> {
            note.put("note", n);
            noteClient.createNote(note);
            System.out.println("Stored note: " + n);
        });
    }

    private String getMultiValue(HashMap<String, String> row, String... fields) {
        List<String> fieldValues = stream(fields).map(row::get).filter(StringUtils::isSet).collect(toList());
        if(fieldValues.isEmpty()) return null;

        String value = fieldValues.get(0);
        if(!fieldValues.stream().allMatch(v -> isEmpty(v) || value.equals(value)))
            throw new IllegalArgumentException("Records do not match! " + String.join(", ", fields));

        return value;
    }

    private void createPeople(CSVData peopleData) throws JsonProcessingException {
        for(HashMap<String, String> row: peopleData.rowData) {
            HashMap<String, Object> person = new HashMap<>(32);
            String name = readName(row);
            add(person, "name", name);

            String familyUID = row.get("Mem Fam Unique ID");
            Map<String, Object> family = knownFamilies.get(familyUID);
            if(family == null)
                throw new IllegalArgumentException("Family not found: " + familyUID);

            if(family.containsKey("id"))
                checkForExistingPerson(family, person);

//            add(person, "family", family);
            add(person, "salutation", row.get("Mem Title"));
            add(person, "suffix", row.get("Mem Suffix"));
            add(person, "maidenName", row.get("Mem Maiden Name"));
            add(person, "nickname", row.get("Mem Nickname"));

            boolean male = mapBoolean(row.get("Mem Gender"), "Male");
            add(person, "male", male);

            add(person, "phoneNumbers", processPhoneNumbers(row.get("Mem Primary Phone"), row.get("Mem Phone List"), male, family));
            add(person, "email", row.get("Mem Email 1"));
            add(person, "headOfHousehold", mapBoolean(row.get("Mem Type"),"Head of Household"));
            add(person, "birthdate", mapDate(row.get("Mem Date of Birth")));

            boolean inactive = mapBoolean(row.get("Mem Inactive"), "YES");
            add(person, "inactive", inactive);
            if(inactive)
                add(person, "inactiveSince", mapDate(row.get("Mem Inactive Date")));

            boolean parishioner = mapBoolean(row.get("Mem Is Church Member"), "YES");
            add(person, "parishioner", parishioner);
            if(parishioner)
                add(person, "memberSince", mapDate(row.get("Mem Date Created")));

//            add(person, "baptized", ??);
//            add(person, "confession", ??);
//            add(person, "communion", ??);
//            add(person, "confirmed", ??);

            add(person, "maritalStatus", mapMaritalStatus(row.get("Mem Marital Status")));
            add(person, "ethnicity", mapEthnicity(row.get("Mem Ethnicity")));
            add(person, "primaryLanguage", mapLanguage(row.get("Mem Language")));
            add(person, "religion", mapReligion(row.get("Mem Religion")));
//            add(person, "specialNeeds", ??);
            add(person, "occupation", row.get("Mem Occupation"));

            addPersonToFamily(person, family);
        }
    }

    private void checkForExistingPerson(Map<String, Object> family, HashMap<String, Object> person) {
        if(!family.containsKey("id"))
            return;

        String personName = (String)person.get("name");
        System.out.println("Checking for existing person record for: " + personName);

        Map<String, Object> existingFamily = familyClient.getFamily((int)family.get("id"));
        if(existingFamily == null)
            throw new IllegalStateException("Family has an id indicating that it already exists.");

        List<Map<String, Object>> members = (List<Map<String, Object>>) existingFamily.get("members");
        if(members == null || members.isEmpty()) {
            System.out.println("Found no family members while checking for existing person record for: " + personName);
            return;
        }

        Optional<Integer> optId = members.stream().filter(m -> personName.equals(m.get("name"))).map(m -> (int)m.get("id")).findFirst();
        if(optId.isPresent()) {
            System.out.println("Found existing person record for: " + personName);
            person.put("id", optId.get());
        } else {
            System.out.println("No existing person record found for: " + personName);
        }
    }

    private String readName(HashMap<String, String> row) {
        String firstName = row.get("Mem First Name");
        String middleName = row.get("Mem Middle Name");
        String lastName = row.get("Mem Last Name");
        return combineName(firstName, middleName, lastName);
    }

    private void generateTemplatePeople(CSVData familyData, CSVData personData) {

        List<Map<String, String>> unmatchedFamilies = familyData.rowData.stream().filter(f -> {
            String famId = f.get("Fam Unique ID");
            return personData.rowData.stream().noneMatch(p -> famId.equals(p.get("Mem Fam Unique ID")));
        }).collect(toList());

        int generatedPeople = 0;
        for(Map<String, String> row: unmatchedFamilies) {
            if(isEmpty(row.get("Fam Spouse First")))
                continue;

            HashMap<String, Object> person = new HashMap<>(32);
            String firstName = row.get("Fam Spouse First");
            String lastName = row.get("Fam Last Name");
            add(person, "name", combineName(firstName, null, lastName));

            String familyUID = row.get("Fam Unique ID");
            Map<String, Object> family = knownFamilies.get(familyUID);
            if(family.containsKey("id"))
                checkForExistingPerson(family, person);


            add(person, "salutation", row.get("Fam Spouse Raw Title"));
            add(person, "suffix", row.get("Fam Spouse Suffix"));
            add(person, "nickname", row.get("Fam Spouse Nickname"));

            boolean male = mapBoolean(row.get("Fam Spouse Gender"), "Male") || mapBoolean(row.get("Fam Spouse Raw Title"), "Mr.");
            add(person, "male", male);

            add(person, "phoneNumbers", processPhoneNumbers(row.get("Fam Primary Phone"), row.get("Fam Phone List"), male, family));
            add(person, "email", row.get("Fam Email List"));
            add(person, "headOfHousehold", true);
            add(person, "birthdate", mapDate(row.get("Fam Spouse Birth Date")));

            boolean inactive = mapBoolean(row.get("Fam Inactive"), "YES");
            add(person, "inactive", inactive);
            if(inactive)
                add(person, "inactiveSince", mapDate(row.get("Fam Inactive Date")));

            boolean parishioner = mapBoolean(row.get("Fam Is Active Church"), "YES");
            add(person, "parishioner", parishioner);
            if(parishioner)
                add(person, "memberSince", mapDate(row.get("Fam Date Created")));

//            add(person, "baptized", ??);
//            add(person, "confession", ??);
//            add(person, "communion", ??);
//            add(person, "confirmed", ??);

            add(person, "maritalStatus", mapMaritalStatus(row.get("Fam Family Status")));
//            add(person, "ethnicity", ??);
            add(person, "primaryLanguage", mapLanguage(row.get("Fam Language")));
//            add(person, "religion", ??);
//            add(person, "specialNeeds", ??);
//            add(person, "occupation", ??);

            addPersonToFamily(person, family);

            generatedPeople++;
        }

        System.out.println("Generated " + generatedPeople + " for " + unmatchedFamilies.size() + " empty families.");
    }

    private void addPersonToFamily(HashMap<String, Object> person, Map<String, Object> family) {
        ((List<Map<String, Object>>)family.computeIfAbsent("members", f -> new LinkedList<Map<String, Object>>())).add(person);
    }

    private void updateRelationships(boolean dryRun) {
        for(Map<String, Object> family: knownFamilies.values()) {
            List<Map<String, Object>> members = ((List<Map<String, Object>>)family.get("members"));
            if (members == null || members.size() < 2)
                continue;

            String personName = (String) members.get(0).get("name");
            if(members.size() > 2) {
                System.err.println("Spouse found in family with " + members.size() + " members! " + personName);
                continue;
            }

            HashMap<String, Object> relationship = new HashMap<>(8);
            relationship.put("personId", members.get(0).get("id"));
            relationship.put("personName", personName);
            relationship.put("otherId", members.get(1).get("id"));
            relationship.put("relationship", "SPOUSE");
            relationship.put("contactPreference", 1);

            if(dryRun) {
                System.out.println("Creating relationship for: " + personName);
            } else {
                relationshipClient.createRelationships(Collections.singletonList(relationship), true);
            }
        }
    }

    private void createFunds(CSVData donationData, boolean dryRun) throws JsonProcessingException {
        int fundsImported = 0;
        Set<String> fundNames = donationData.rowData.stream().map(r -> r.get("Fund Hist Activity")).filter(StringUtils::isSet).collect(Collectors.toSet());
        for(String fundName: fundNames) {
            HashMap<String, Object> fund = new HashMap<>(32);
            add(fund, "name", fundName);

            fundsImported++;

            Map<String, Object> createdFund;
            if(dryRun) {
                System.out.println("Processed line: " + new ObjectMapper().writeValueAsString(fund));
                createdFund = fund;
                createdFund.put("id", fundsImported);
            } else {
                createdFund = findOrCreateFund(fund);
            }
            knownFunds.put((String)createdFund.get("name"), (Integer)createdFund.get("id"));
        }
    }

    private Map<String, Object> findOrCreateFund(HashMap<String, Object> fund) {
        int fundId = fundClient.getFundId((String) fund.get("name"));
        if(fundId != 0) {
            fund.put("id", fundId);
            return fund;
        } else
            return fundClient.createFund(fund);
    }

    private void importDonations(CSVData donationData, boolean dryRun) throws JsonProcessingException {
        int donationsImported = 0;
        int totalDonations = donationData.rowData.size();
        int processingLine = 0;
        long start = System.currentTimeMillis();

        List<Map<String, Object>> batch = new LinkedList<>();
        for(HashMap<String, String> row: donationData.rowData) {
            processingLine++;

            String familyUID = row.get("Fam Unique ID");

            String fundName = row.get("Fund Hist Activity");
            String amount = row.get("Fund Hist Amount");

            if(isEmpty(fundName)) {
                if (isEmpty(amount) || amount.equals("0.00") || amount.equals("0")) {
                    System.out.println("Found bad donation record... Skipping");
                    continue;
                } else {
                    throw new RuntimeException(String.format("Found donation with value (%s) but no fund name.", amount));
                }
            }

            Map<String, Object> family = knownFamilies.get(familyUID);
            if(family == null || family.get("id") == null) {
                System.err.println(String.format("Donation on %s cannot be mapped to family %s", row.get("Fund Hist Date"), familyUID));
                continue;
            }

            HashMap<String, Object> donation = new HashMap<>(32);
            add(donation, "familyId", family.get("id"));
            add(donation, "fundId", knownFunds.get(fundName));
            add(donation, "amount", amount);
            add(donation, "deductibleAmount", amount);
            add(donation, "batchNumber", row.get("Fund Hist Batch Number"));
            LocalDate donationDate = mapDate(row.get("Fund Hist Date"));
            if(donationDate.isAfter(LocalDate.now())) {
                System.err.println(String.format("Skipping: Donation recorded for future date %s for family %s", row.get("Fund Hist Date"), familyUID));
                continue;
            }
            add(donation, "donationDate", donationDate);
            add(donation, "donationType", "UNKNOWN");
            add(donation, "notes", row.get("Fund Hist Comment"));

            ++donationsImported;

            Map<String, Object> createdDonation;
            if(dryRun) {
                System.out.println("Processed line: " + new ObjectMapper().writeValueAsString(donation));
                createdDonation = donation;
                createdDonation.put("id", donationsImported);
            } else {
                batch.add(donation);

                if(batch.size() > 99) {
                    donationClient.createDonations(batch);
                    batch.clear();
                    System.out.println(String.format("Batch processed. %.4f (%d/%d) elapsed time: %d minutes.", (processingLine*1.0f)/totalDonations, processingLine, totalDonations, (System.currentTimeMillis() - start)/60000));
                }
            }
        }

        if(batch.size() > 0)
            donationClient.createDonations(batch);
    }

    private void add(Map<String, Object> map, String field, Object value) {
        if(value != null) map.put(field, value);
    }

    private HashMap<String, Object> processAddress(String addrString) {
        if(isEmpty(addrString)) return null;

        String[] addrBits = addrString.split("\\|");

        if(addrBits.length > 3 || addrBits.length < 2)
            throw new IllegalArgumentException("Could not parse address info from: " + addrString);

        HashMap<String, Object> addr = new HashMap<>(8);
        addr.put("street1", addrBits[0]);
        if(addrBits.length == 3)
            addr.put("street2", addrBits[1]);

        Matcher cszMatcher = CITY_STATE_ZIP_PARSER.matcher(addrBits[addrBits.length-1]);

        if(!cszMatcher.matches())
            throw new IllegalArgumentException("Could not pare City, State, Zip info from: " + addrBits[addrBits.length-1]);

        addr.put("city", cszMatcher.group(1));
        addr.put("state", cszMatcher.group(3));
        addr.put("zip", Integer.parseInt(cszMatcher.group(4)));

        return addr;
    }

    private List<HashMap<String, Object>> processPhoneNumbers(String primary, String phoneList, boolean male, Map<String, Object> family) {
        if(isEmpty(phoneList)) return null;

        List<HashMap<String, Object>> results = new LinkedList<>();
        String[] phoneBits = phoneList.split("\\|");
        for(String entry: phoneBits) {
            String[] bits = splitEntry(entry.trim());
            String phone = bits[0];
            String type = bits[1];

            if (isSet(type)) {
                if(type.equals("Home Ph")) {
                    add(family, "homePhone", phone);
                    continue;
                } else if (male && isHers(type) || !male && isHis(type)) {
                    continue;
                }
            }

            HashMap<String, Object> phoneRecord = new HashMap<>(4);
            add(phoneRecord, "phoneNumber", phone);
            add(phoneRecord, "type", mapPhoneType(type));
            add(phoneRecord, "primary", entry.trim().equals(primary));
            results.add(phoneRecord);
        }
        return results;
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\(\\d{3}\\)\\s\\d{3}-\\d{4}(\\s?x\\s?\\d+)?)\\s?(.*)");

    private String[] splitEntry(String entry) {
        Matcher match = PHONE_PATTERN.matcher(entry);

        if(!match.matches())
            throw new IllegalArgumentException("Invalid phone string encountered: " + entry);

        String phone = match.group(1);
        String type = match.group(3);

        return new String[] {phone, type};
    }

    private boolean isHers(String type) {
        return type.contains("Her") || type.contains("Wife");
    }

    private boolean isHis(String type) {
        return type.contains("His") || type.contains("Husband");
    }

    // Data mapping
    private boolean mapBoolean(String input, String postiveValue) {
        return !isEmpty(input) && input.equalsIgnoreCase(postiveValue);
    }

    private LocalDate mapDate(String dateString) {
        return isEmpty(dateString)? null: LocalDate.parse(dateString, DateTimeFormatter.ofPattern("M/d/yyyy"));
    }

    private String mapReligion(String input) {
        if(isEmpty(input)) return null;
        switch (input) {
            case "Catholic":
                return input.toUpperCase();
            default:
                throw new IllegalArgumentException("Could not map Religion: " + input);
        }
    }

    private String mapEthnicity(String input) {
        if(isEmpty(input)) return null;
        switch (input) {
            default:
                throw new IllegalArgumentException("Could not map Ethnicity: " + input);
        }
    }

    private String mapLanguage(String input) {
        if(isEmpty(input)) return null;
        switch (input) {
            case "English":
            case "Spanish":
                return input.toUpperCase();
            default:
                throw new IllegalArgumentException("Could not map Language: " + input);
        }
    }


    private String mapMaritalStatus(String input) {
        if(isEmpty(input)) return null;
        switch (input) {
            case "Church Marriage":
                return "MARRIED_IN_CHURCH";
            case "Married":
            case "Widowed":
                return input.toUpperCase();
            default:
                throw new IllegalArgumentException("Could not map Marital Status: " + input);
        }
    }

    private String mapPhoneType(String input) {
        if(isEmpty(input)) return "OTHER";
        switch (input) {
            case "His cell":
            case "his cell":
            case "Her cell":
            case "her cell":
            case "Wife cell":
            case "Husband Cell":
            case "Cell":
            case "Cellular":
                return "CELL";
            case "Home Ph":
                return "HOME";
            case "Work":
            case "Work Her":
            case "Her work":
            case "Her Work":
            case "Unl. Her Work":
            case "her work":
            case "His work":
            case "his work":
            case "Unl. his work":
            case "Work He":
            case "Work-He":
            case "Work-C":
                return "WORK";
            case "Sch Ofc":
            case "Her fax":
            case "his fax":
            case "Fax":
            case "Gregg-W": // WTF?
            case "Barry":
                return "OTHER";
            default:
                throw new IllegalArgumentException("Could not map Phone Type: " + input);
        }
    }

    private String combineName(String first, String middle, String last){
        return isSet(middle)?
                String.format("%s %s %s", first, middle, last):
                String.format("%s %s", first, last);
    }
}
