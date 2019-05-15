package com.servantscode.fakedata.integration.serviceu;

import com.servantscode.fakedata.client.EventServiceClient;
import com.servantscode.fakedata.client.RoomServiceClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class ServiceuScheduleImport {
    public static void main(String[] args) throws IOException {
//        File importFile = new File("c:\\Users\\gleit\\stgabriel\\events-test.csv");
        File importFile = new File("c:\\Users\\gleit\\stgabriel\\events-5-14.csv");

        new ServiceuScheduleImport().processFile(importFile, true, 1);
    }

    private RoomServiceClient roomClient;
    private EventServiceClient eventClient;

    public ServiceuScheduleImport() {
        roomClient = new RoomServiceClient();
        eventClient = new EventServiceClient();
    }

    private void processFile(File importFile, boolean hasHeaders, int schedulerId) {
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

            while((line = fileLines.readLine()) != null) {
                String[] parsedLine = parseCsvLine(line);

                //Horray for fixed field structures!
                String date = parsedLine[0];
                String title = parsedLine[1];
                String description = parsedLine[2];
                String eventTimes = parsedLine[3];
                String resourceTimes = parsedLine[4];
                String resourceString = parsedLine[5];

                if(isSet(date)) {
                    currentDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("\"EEEE, LLLL d, yyyy\""));
                    System.out.println("Current Date is now: " + currentDate);
                }

                if(isEmpty(eventTimes)) {
                    badLines.add(line);
                    continue;
                }

                ZonedDateTime[] eventDateTimes = parseTimes(eventTimes, currentDate);
                System.out.println(String.format("Event runs from: %s to %s", eventDateTimes[0].format(ISO_OFFSET_DATE_TIME), eventDateTimes[1].format(ISO_OFFSET_DATE_TIME)));

                if(isEmpty(resourceTimes) && isSet(resourceString)) {
                    badLines.add(line);
                    continue;
                }

                ZonedDateTime[] resDateTimes = parseTimes(resourceTimes, currentDate);
                System.out.println(String.format("Reservations runs from: %s to %s", resDateTimes[0].format(ISO_OFFSET_DATE_TIME), resDateTimes[1].format(ISO_OFFSET_DATE_TIME)));

                String[] resources = parseResources(resourceString);
                for(String res: resources) {
                    System.out.println("  Reserving: [" + res + "]");
                    rooms.add(res);
                }

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

            createRooms(rooms);
            populateReservations(events, schedulerId);
            createReservations(events);

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

    private void populateReservations(List<HashMap<String, Object>> events, int schedulerId) {
        for(HashMap<String, Object> event: events) {
            ZonedDateTime[] times = (ZonedDateTime[]) event.remove("reservationTimes");
            String[] resources = (String[]) event.remove("reservations");

            List<HashMap<String, Object>> reservations = new ArrayList<>(resources.length);
            for(String resource: resources) {
                HashMap<String, Object> res = new HashMap<>(8);
                res.put("resourceType", "ROOM");
                int roomId = roomClient.getRoomId(resource);
                System.out.println(String.format("Found room id %d for room %s", roomId, resource));
                res.put("resourceId", roomId);
                res.put("reservingPersonId", schedulerId);
                res.put("startTime", times[0]);
                res.put("endTime", times[1]);
                reservations.add(res);
            }

            event.put("reservations", reservations);
        }
    }

    private void createReservations(List<HashMap<String, Object>> events) {
        events.forEach(event -> eventClient.createEvent(event));
    }

    // ----- Parser bits -----
    public String[] parseCsvLine(String line) {
        boolean quote=false;

        char[] chars = line.trim().toCharArray();
        List<String> fields = new LinkedList<>();
        int start = 0;
        for(int i=0; i<chars.length; i++) {
            switch (chars[i]) {
                case ',':
                    if(quote)
                        break;

                    String field = new String(chars, start, i-start).trim();
                    fields.add(field);
                    start=i+1;
                    break;
                case '\"':
                    quote = !quote;
                    break;
            }
        }
        if(start < chars.length) {
            if(quote)
                throw new RuntimeException("Could not parse csv line.");

            String field = new String(chars, start, chars.length-start).trim();
            fields.add(field);
        } else {
            //Empty last field
            fields.add("");
        }

        return fields.toArray(new String[fields.size()]);
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
