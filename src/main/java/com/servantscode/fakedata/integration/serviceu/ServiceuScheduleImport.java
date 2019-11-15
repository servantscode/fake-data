package com.servantscode.fakedata.integration.serviceu;

import com.servantscode.fakedata.client.EventServiceClient;
import com.servantscode.fakedata.client.PersonServiceClient;
import com.servantscode.fakedata.client.RoomServiceClient;
import com.servantscode.fakedata.integration.CSVParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.stream;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class ServiceuScheduleImport {
    public static void main(String[] args) throws IOException {
        String filePath = null;
        File importFile = new File(filePath);

//        BaseServiceClient.setUrlPrefix("https://<parish>.servantscode.org");
//        BaseServiceClient.login("user", "password");
        new ServiceuScheduleImport().processFile(importFile, true);
    }

    private RoomServiceClient roomClient;
    private EventServiceClient eventClient;
    private PersonServiceClient personClient;

    public ServiceuScheduleImport() {
        roomClient = new RoomServiceClient();
        eventClient = new EventServiceClient();
        personClient = new PersonServiceClient();
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

            Set<String> rooms = new HashSet<>();
            List<HashMap<String, Object>> events = new LinkedList<>();
            LocalDate currentDate = null;

            List<String> badLines = new LinkedList<>();

            Set<String> departments = new HashSet<>();
            Set<String> categories = new HashSet<>();
            while((line = fileLines.readLine()) != null) {
                String[] parsedLine = CSVParser.parseCsvLine(line);

                //Horray for fixed field structures!
                String date = parsedLine[0];
                String title = parsedLine[1];
                String description = parsedLine[2];
//                String confirmationNumber = parsedLine[3];
                String eventTimes = parsedLine[4];
                String resourceTimes = parsedLine[5];
                String resourceString = parsedLine[6];
//                String location = parsedLine[7];
//                String approvalStatus = parsedLine[8];
//                String visibility = parsedLine[9];
                String department = parsedLine[10];
                String category = parsedLine[11];
                String submittedBy = parsedLine[12];

                if(isSet(department.trim()))
                    departments.addAll(stream(department.split("\\|")).map(String::trim).collect(Collectors.toList()));
                if(isSet(category.trim()))
                    categories.addAll(stream(category.split("\\|")).map(String::trim).collect(Collectors.toList()));

                if(isSet(date)) {
                    currentDate = LocalDate.parse(date, ofPattern("\"EEEE, LLLL d, yyyy\""));
                    System.out.println("Current Date is now: " + currentDate);
                }

                if(isEmpty(eventTimes)) {
                    eventTimes = "12:00AM - 11:59PM";
                }

                ZonedDateTime[] eventDateTimes = parseTimes(eventTimes, currentDate);
                System.out.println(String.format("Event runs from: %s to %s", eventDateTimes[0].format(ISO_OFFSET_DATE_TIME), eventDateTimes[1].format(ISO_OFFSET_DATE_TIME)));

                if(isEmpty(resourceTimes)) {
                    resourceTimes = "12:00AM - 11:59PM";
                }

                ZonedDateTime[] resDateTimes = parseTimes(resourceTimes, currentDate);
//                System.out.println(String.format("Reservations runs from: %s to %s", resDateTimes[0].format(ISO_OFFSET_DATE_TIME), resDateTimes[1].format(ISO_OFFSET_DATE_TIME)));

                String[] resources = parseResources(resourceString);
                for(String res: resources) {
//                    System.out.println("  Reserving: [" + res + "]");
                    rooms.add(res);
                }

                int schedulerId = personClient.getPersonId(submittedBy);
                if(schedulerId == 0)
                    schedulerId = 3;

                HashMap<String, Object> event = new HashMap<>(16);
                event.put("title", title);
                event.put("description", description);
                event.put("startTime", eventDateTimes[0]);
                event.put("endTime", eventDateTimes[1]);
                event.put("schedulerId", schedulerId);
                event.put("reservations", resources);
                event.put("reservationTimes", resDateTimes);
                events.add(event);

                lineNumber++;
            }

            System.out.println("Departments: \"" + String.join("\", \"", departments));
            System.out.println("Categories: \"" + String.join("\", \"", categories));


//            createRooms(rooms);
//            populateReservations(events);
//            events = new RecurrenceProcessor().identifyRecurrences(events);
//            createEvents(events);

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

    private void createRooms(Set<String> rooms) {
        System.out.println("Rooms found: " + rooms.size());
        for(String room: rooms) {
            if(isEmpty(room))
                continue;

            HashMap<String, Object> roomObj = new HashMap<>(4);
            roomObj.put("name", room);
            roomObj.put("type", "OTHER");
            roomObj.put("capacity", 0);
            roomClient.createRoom(roomObj);
        }
    }

    private void populateReservations(List<HashMap<String, Object>> events) {
        for(HashMap<String, Object> event: events) {
            ZonedDateTime[] times = (ZonedDateTime[]) event.remove("reservationTimes");
            String[] resources = (String[]) event.remove("reservations");

            List<HashMap<String, Object>> reservations = new ArrayList<>(resources.length);
            for(String resource: resources) {
                HashMap<String, Object> res = new HashMap<>(8);
                res.put("resourceType", "ROOM");
                res.put("resourceId", roomClient.getRoomId(resource));
                res.put("reservingPersonId", event.get("schedulerId"));
                res.put("startTime", times[0]);
                res.put("endTime", times[1]);
                reservations.add(res);
            }

            event.put("reservations", reservations);
        }
    }

    private void createEvents(List<HashMap<String, Object>> events) {
        events.forEach(event -> eventClient.createEvent(event));
    }

    private ZonedDateTime[] parseTimes(String eventTimes, LocalDate currentDate) {
        String[] times = eventTimes.split("-");
        ZonedDateTime startTime = parseTime(currentDate, times[0].trim());
        ZonedDateTime endTime = parseTime(currentDate, times[1].trim());
        return new ZonedDateTime[] {startTime, endTime};
    }

    private ZonedDateTime parseTime(LocalDate currentDate, String time) {
        String[] timeBits = time.split(":");
        int hours = Integer.parseInt(timeBits[0]) + (timeBits[1].substring(2).toLowerCase().startsWith("p")? 12: 0);
        if(hours % 12 == 0) hours -= 12;
        int minutes = Integer.parseInt(timeBits[1].substring(0,2));
        return currentDate.atTime(hours, minutes).atZone(ZoneId.systemDefault());
    }

    private String[] parseResources(String resourceString) {
        return resourceString.split("\\s*\\|\\s*");
    }
}
