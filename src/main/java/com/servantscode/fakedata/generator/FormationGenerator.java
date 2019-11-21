package com.servantscode.fakedata.generator;

import org.servantscode.client.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.util.Arrays.asList;

public class FormationGenerator {
    private final ProgramGroupServiceClient programGroupClient;
    private final ProgramServiceClient programClient;
    private final PersonServiceClient personClient;
    private final RoomServiceClient roomClient;
    private final EventServiceClient eventClient;
    private final PersonSelector personSelector;

    public FormationGenerator() {
        programGroupClient = new ProgramGroupServiceClient();
        programClient = new ProgramServiceClient();
        personClient = new PersonServiceClient();
        roomClient = new RoomServiceClient();
        eventClient = new EventServiceClient();
        personSelector = new PersonSelector();
    }

    Map<String, Object> lastSchoolYear;
    Map<String, Object> thisSchoolYear;

    public static void generate() throws IOException {
        FormationGenerator generator = new FormationGenerator();
        generator.generateProgramGroups();
    }

// Program Groups
    private void generateProgramGroups() {
        lastSchoolYear = generateProgramGroup("2018-2019", true);
//        generatePrograms(lastSchoolYear);

        thisSchoolYear = generateProgramGroup("2019-2020", false);
        generatePrograms(thisSchoolYear);
    }

    private Map<String, Object> generateProgramGroup(String name, boolean complete)  {
        HashMap<String, Object> group = new HashMap<>(4);
        group.put("name", name);
        group.put("complete", complete);
        return programGroupClient.createProgramGroup(group);
    }

// Programs
    private void generatePrograms(Map<String, Object> group) {
        createProgram("Religious Education", (int)group.get("id"), asList("Kindergarten", "1st", "2nd", "3rd", "4th", "5th"));
        createProgram("Youth Ministry", (int)group.get("id"), asList("6th", "7th", "8th"));
        createProgram("High School Ministry", (int)group.get("id"), asList("9th", "10th", "11th", "12th"));
    }

    private Map<String, Object> createProgram(String name, int groupId, List<String> classes) {
        Map<String, Object> program = new HashMap<>(4);
        program.put("name", name);
        program.put("groupId", groupId);
        program.put("coordinatorId", personSelector.randomAdult().get("id"));

        program = programClient.createProgram(program);
        List<Map<String, Object>> sections = generateSections(program, classes);
        linkSessions(program, LocalDate.now());
        Map<Integer, List<Map<String, Object>>> registrations = generateRegistrations(sections, 10);
        generateAttendance(sections, registrations);

        return program;
    }

    private List<Map<String, Object>> generateSections(Map<String, Object> program, List<String> classes) {
        SectionServiceClient sectionClient = new SectionServiceClient((int) program.get("id"));

        List<Map<String, Object>> sections = new LinkedList<>();
        List<Integer> classRoomIds = randomClassRooms(classes.size());
        int roomIter = 0;
        for(String name: classes) {
            Map<String, Object> section = new HashMap<>();
            section.put("name", name);
            section.put("programId", program.get("id"));
            section.put("instructorId", personSelector.randomAdult().get("id"));
            section.put("roomId", classRoomIds.get(roomIter++));

            sections.add(sectionClient.createSection(section));
        }

        return sections;
    }

// Sessions
    private void linkSessions(Map<String, Object> program, LocalDate aroundDate) {
        SessionServiceClient sessionClient = new SessionServiceClient((int) program.get("id"));

        String eventSearch = String.format("%s startTime:[%s TO %s]", program.get("name"), aroundDate.format(ISO_DATE), aroundDate.plusMonths(1).format(ISO_DATE));
        int recurrenceId = (int)((Map<String,Object>)eventClient.getEvent(eventSearch).get("recurrence")).get("id");

        Map<String, Object> sessions = new HashMap<>();
        sessions.put("programId", program.get("id"));
        sessions.put("recurrenceId", recurrenceId);
        sessionClient.createSession(sessions);
    }

// Registrations
    private Map<Integer, List<Map<String, Object>>> generateRegistrations(List<Map<String, Object>> sections, int kidsPer) {
        int programId = (int) sections.get(0).get("programId");
        RegistrationServiceClient registrationClient = new RegistrationServiceClient(programId);
        LocalDate yearStart = LocalDate.now().withMonth(9).withDayOfMonth(1);
        Map<Integer, List<Map<String, Object>>> registrationsBySection = new HashMap<>();

        for(Map<String, Object> section: sections) {
            List<Map<String, Object>> registrations = new LinkedList<>();
            int sectionId = (int) section.get("id");

            int age = getAgeFor((String) section.get("name"));
            String query = String.format("birthdate:[%s TO %s]", yearStart.minusYears(age).format(ISO_DATE), yearStart.minusYears(age-1).format(ISO_DATE));
            List<Integer> childIds = personClient.getPersonIds(query);
            if(childIds.size() > kidsPer)
                childIds = RandomSelector.select(childIds, kidsPer);

            Map<String, Object> reg = new HashMap<>();
            reg.put("programId", programId);
            reg.put("sectionId", sectionId);
            reg.put("schoolGrade", gradeNumberForSection((String) section.get("name")));
            for(int child: childIds){
                reg.put("enrolleeId", child);
                registrations.add(registrationClient.createRegistration(reg));
            }
            registrationsBySection.put(sectionId, registrations);
        }
        return registrationsBySection;
    }

// Attendance
    private void generateAttendance(List<Map<String, Object>> sections, Map<Integer, List<Map<String, Object>>> registrations) {
        int programId = (int) sections.get(0).get("programId");
        SessionServiceClient sessionClient = new SessionServiceClient(programId);
        AttendanceServiceClient attendanceClient = new AttendanceServiceClient(programId);
        List<Map<String, Object>> sessions = sessionClient.getPastSessions();

        for(Map<String, Object> section: sections) {
            int sectionId = (int) section.get("id");
            Map<String, Object> attendance = new HashMap<>();
            attendance.put("programId", programId);
            attendance.put("sectionId", sectionId);
            for(Map<String, Object> session: sessions) {
                attendance.put("sessionId", session.get("id"));
                attendance.put("enrolleeAttendance", generateEnrolleeAttendance(registrations.get(sectionId)));
                attendanceClient.recordAttendance(attendance, sectionId);
            }
        }
    }

    private Map<Integer, Boolean> generateEnrolleeAttendance(List<Map<String, Object>> registrations) {
        Map<Integer, Boolean> attendance = new HashMap<>();
        for(Map<String, Object> reg: registrations)
            attendance.put((Integer) reg.get("enrolleeId"), RandomSelector.byPercentage(.75f));
        return attendance;
    }


    // Utilities
    private List<Integer> classRoomIds = null;
    private List<Integer> randomClassRooms(int size) {
        if(classRoomIds == null)
            classRoomIds = roomClient.getClassRoomIds();

        return RandomSelector.select(classRoomIds, size);
    }

    private int getAgeFor(String gradeName){
        String grade = gradeNumberForSection(gradeName);
        return grade.equals("K")? 5: Integer.parseInt(grade)+5;
    }

    private String gradeNumberForSection(String gradeName) {
        if(gradeName.equals("Kindergarten"))
            return "K";
        return gradeName.replaceAll("\\D", "");
    }

}
