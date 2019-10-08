package com.servantscode.fakedata.generator;

import com.servantscode.fakedata.client.EquipmentServiceClient;
import com.servantscode.fakedata.client.EventServiceClient;
import com.servantscode.fakedata.client.MinistryServiceClient;
import com.servantscode.fakedata.client.RoomServiceClient;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static com.servantscode.fakedata.generator.RandomSelector.*;
import static java.time.DayOfWeek.*;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static java.util.Arrays.asList;

public class EventGenerator {
    private static final EventServiceClient eventClient = new EventServiceClient();
    private static final RoomServiceClient roomClient = new RoomServiceClient();
    private static final EquipmentServiceClient equipmentClient = new EquipmentServiceClient();
    private static final MinistryServiceClient ministryClient = new MinistryServiceClient();

    private static List<String> reMinistries = asList("Religious Education", "St. Ann Youth");
    private static List<String> massMinistries = asList("Alter Linens", "Alter Servers", "Children's Liturgy of the Word",
                "Extrodanary Ministers of Holy Communion", "Lectors", "Ushers", "Greeters");

    private static HashMap<String, Integer> meetingFreq;

    static {
        meetingFreq = new HashMap<>(8);
        meetingFreq.put("WEEKLY", 60);
        meetingFreq.put("DAY_OF_MONTH", 5);
        meetingFreq.put("WEEKDAY_OF_MONTH", 30);
        meetingFreq.put("YEARLY", 5);
    }

    public static void generate() {
        generateMasses();
        generateRE();
        generateMinistryMeetings();
    }

    private static void generateMinistryMeetings() {
        List<String> ministries = ministryClient.getMinistries();
        ministries.removeAll(reMinistries);

        ministries.forEach(EventGenerator::generateMinistryMeetings);
    }

    private static void generateMinistryMeetings(String ministry) {
        ZonedDateTime start = randomTime(startOfYear());
        ZonedDateTime end = start.plusMinutes((rand.nextInt(6) + 1)*30);
        boolean schoolCycle = rand.nextBoolean();

        Map<String, Object> recurrence = randomRecurrence(schoolCycle);
        List<Map<String, Object>> reservations = randomReservations(start, end, 1, Math.max(0, rand.nextInt(5)-2));
        if(schoolCycle)
            generateSchoolYearEvent(ministry + " Meeting", ministry, start, end, recurrence, reservations);
        else
            generateAllYearEvent(ministry + " Meeting", ministry, start, end, recurrence, reservations);
    }

    private static List<Map<String, Object>> randomReservations(ZonedDateTime start, ZonedDateTime end, int rooms, int equipment) {
        List<Map<String, Object>> reservations = new ArrayList<>(rooms+equipment);
        reservations.addAll(generateRoomReservations(start, end, select(roomClient.getRooms(), rooms)));
        reservations.addAll(generateEquipmentReservations(start, end, select(equipmentClient.getEquipment(), equipment)));
        return reservations;
    }

    private static void generateRE() {
        List<String> rooms = roomClient.getClassRooms();
        generateREEvent("Religious Education", "Religious Education", 4, rooms, asList(TUESDAY));
        generateREEvent("Sunday School", "Religious Education", 8, rooms, asList(SUNDAY));
        generateREEvent("Sunday School", "Religious Education", 10, rooms, asList(SUNDAY));
        generateREEvent("Sunday School", "Religious Education", 12, rooms, asList(SUNDAY));
        generateREEvent("Youth Ministry", "Youth Ministry", 5, rooms, asList(SUNDAY));
        generateREEvent("High School Ministry", "High School Ministry", 18, rooms, asList(WEDNESDAY));
    }

    private static void generateREEvent(String title, String ministry, int time, List<String> rooms, List<DayOfWeek> days) {
        ZonedDateTime start = startOfYear().withHour(time);
        ZonedDateTime end = start.plusMinutes(90);

        generateSchoolYearEvent(title, ministry, start, end, generateWeeklyRecurrance(days, endOfMay()),
                                generateRoomReservations(start, end, rooms));
    }

    private static void generateSchoolYearEvent(String title, String ministry, ZonedDateTime start, ZonedDateTime end,
                                                     Map<String, Object> recurrence, List<Map<String, Object>> reservations) {
        Map<String, Object> eventData = generateEvent(title, ministry, start, end, recurrence, reservations);
        eventClient.createEvent(eventData);

        ZonedDateTime newStart = start.withMonth(9).withDayOfMonth(7);
        Duration addedTime = Duration.between(start, newStart);
        ZonedDateTime newEnd = end.plus(addedTime);

        eventData.put("startTime", newStart);
        eventData.put("endTime", newEnd);

        recurrence.put("endDate", endOfYear().toLocalDate());
        eventData.put("recurrence", recurrence);

        reservations.forEach((res) -> {
            res.put("startTime", addTime(res.get("startTime"), addedTime));
            res.put("endTime", addTime(res.get("endTime"), addedTime));
        });
        eventData.put("reservations", reservations);
        eventClient.createEvent(eventData);
    }


    private static void generateAllYearEvent(String title, String ministry, ZonedDateTime start, ZonedDateTime end,
                                             Map<String, Object> recurrence, List<Map<String, Object>> reservations) {

        eventClient.createEvent(generateEvent(title, ministry, start, end, recurrence, reservations));
    }

    private static Map<String, Object> generateEvent(String title, String ministry, ZonedDateTime start, ZonedDateTime end,
                                                     Map<String, Object> recurrence, List<Map<String, Object>> reservations) {
        int ministryId = ministryClient.getMinistryId(ministry);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", title);
        eventData.put("startTime", start);
        eventData.put("endTime", end);
        eventData.put("schedulerId", 1);
        eventData.put("ministryId", ministryId);

        eventData.put("recurrence", recurrence);
        eventData.put("reservations", reservations);

        return eventData;
    }

    // Mass Helpers
    private static void generateMasses() {
        generateSundayMass(17, SATURDAY);
        generateSundayMass(8);
        generateSundayMass(10);
        generateSundayMass(12);
        generateSundayMass(14);
        generateDailyMass(7, "Chapel", asList(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY));
        generateDailyMass(12, "Chapel", asList(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY));
        generateDailyMass(8, "Chapel", asList(SATURDAY));
    }

    private static void generateSundayMass(int time) {
        generateMass(time, 90, "Main Church", Collections.singletonList(SUNDAY));
    }

    private static void generateSundayMass(int time, DayOfWeek day) {
        generateMass(time, 90, "Main Church", Collections.singletonList(day));
    }

    private static void generateDailyMass(int time, String roomName, List<DayOfWeek> days) {
        generateMass(time, 40, roomName, days);
    }

    private static void generateMass(int time, int length, String roomName, List<DayOfWeek> days) {
        ZonedDateTime start = startOfYear().withHour(time);
        ZonedDateTime end = start.plusMinutes(length);

        Map<String, Object> massData = new HashMap<>();
        massData.put("title", "Mass");
        massData.put("startTime", start);
        massData.put("endTime", end);
        massData.put("schedulerId", 1);

        massData.put("recurrence", generateWeeklyRecurrance(days));
        massData.put("reservations", generateRoomReservations(start, end, asList(roomName)));

        eventClient.createEvent(massData);
    }

    private static List<Map<String, Object>> generateRoomReservations(ZonedDateTime start, ZonedDateTime end, List<String> rooms) {
        return rooms.stream().
                map((room) -> generateReservation("ROOM", roomClient.getRoomId(room), start, end)).
                collect(Collectors.toList());
    }

    private static List<Map<String, Object>> generateEquipmentReservations(ZonedDateTime start, ZonedDateTime end, List<String> gear) {
        return gear.stream().
                map((item) -> generateReservation("EQUIPMENT", equipmentClient.getEquipmentId(item), start, end)).
                collect(Collectors.toList());
    }

    private static Map<String, Object> generateReservation(String type, int id, ZonedDateTime start, ZonedDateTime end) {
        Map<String, Object> reservationData = new HashMap<>();
        reservationData.put("resourceType", type);
        reservationData.put("resourceId", id);
        reservationData.put("reservingPersonId", 1);
        reservationData.put("startTime", start.minusMinutes(10));
        reservationData.put("endTime", end.plusMinutes(10));
        return reservationData;
    }

    // Recurrence Helpers
    private static Map<String, Object> randomRecurrence(boolean schoolCycle) {
        String recurType = weightedSelect(meetingFreq);
        int recurTime = Math.max(1, rand.nextInt(10)-7);

        ZonedDateTime end = schoolCycle? endOfMay() : startOfDay(ZonedDateTime.now().with(lastDayOfYear()));

        int numberOfDaysPerWeek = Math.max(1, rand.nextInt(7)-3);
        Set<DayOfWeek> days = new HashSet<>();
        while(days.size() < numberOfDaysPerWeek)
            days.add(randomDay());

        return generateRecurrence(recurType, recurTime, end, days);
    }

    private static Map<String, Object> generateWeeklyRecurrance(List<DayOfWeek> days) {
        return generateWeeklyRecurrance(days, endOfYear());
    }

    private static Map<String, Object> generateWeeklyRecurrance(List<DayOfWeek> days, ZonedDateTime end) {
        return generateRecurrence("WEEKLY", 1, end, days);
    }

    private static Map<String, Object> generateRecurrence(String cycle, int freq, ZonedDateTime end, Collection<DayOfWeek> days) {
        Map<String, Object> recurranceData = new HashMap<>();
        recurranceData.put("cycle", cycle);
        recurranceData.put("frequency", freq);
        recurranceData.put("endDate", end.toLocalDate());
        recurranceData.put("weeklyDays", days);

        return recurranceData;
    }

    // Date Helpers
    private static ZonedDateTime addTime(Object input, Duration addedTime) {
        ZonedDateTime time = (input instanceof ZonedDateTime)? (ZonedDateTime)input : ZonedDateTime.parse((String)input);
        return time.plus(addedTime);
    }

    private static ZonedDateTime endOfMay() {
        return startOfDay(ZonedDateTime.now().withMonth(5).withDayOfMonth(30));
    }

    private static ZonedDateTime startOfYear() {
        return startOfDay(ZonedDateTime.now().with(TemporalAdjusters.firstDayOfYear()));
    }

    private static ZonedDateTime endOfYear() {
        return startOfDay(ZonedDateTime.now().with(lastDayOfYear()));
    }

    private static ZonedDateTime startOfDay(ZonedDateTime input) {
        return input.toLocalDate().atStartOfDay(input.getZone());
    }
}
