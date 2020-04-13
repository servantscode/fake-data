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

        Map<String, Object> section = generateSection(program, LocalDate.now());

        List<Map<String, Object>> classrooms = generateClassrooms(program, section, classes);
        Map<Integer, List<Map<String, Object>>> registrations = generateRegistrations(classrooms, 10);
        generateAttendance(classrooms, registrations);

        return program;
    }


    private List<Map<String, Object>> generateClassrooms(Map<String, Object> program, Map<String, Object> section, List<String> classes) {
        ClassroomServiceClient classroomClient = new ClassroomServiceClient((int) program.get("id"), (int)section.get("id"));

        List<Map<String, Object>> classrooms = new LinkedList<>();
        List<Integer> classRoomIds = randomClassRooms(classes.size());
        int roomIter = 0;
        for(String name: classes) {
            Map<String, Object> classroom = new HashMap<>();
            classroom.put("name", name);
            classroom.put("programId", program.get("id"));
            classroom.put("sectionId", section.get("id"));
            classroom.put("instructorId", personSelector.randomAdult().get("id"));
            classroom.put("roomId", classRoomIds.get(roomIter++));

            classrooms.add(classroomClient.createClassroom(classroom));
        }

        return classrooms;
    }

// Sessions
    private Map<String, Object> generateSection(Map<String, Object> program, LocalDate aroundDate) {
        SectionServiceClient sectionClient = new SectionServiceClient((int) program.get("id"));
        Map<String, Object> section =  sectionClient.getDefaultSection();

        String eventSearch = String.format("%s startTime:[%s TO %s]", program.get("name"), aroundDate.format(ISO_DATE), aroundDate.plusMonths(1).format(ISO_DATE));
        int recurrenceId = (int)((Map<String,Object>)eventClient.getEvent(eventSearch).get("recurrence")).get("id");

        section.put("recurrenceId", recurrenceId);
        sectionClient.updateSection(section);
        return section;
    }

// Registrations
    private Map<Integer, List<Map<String, Object>>> generateRegistrations(List<Map<String, Object>> classrooms, int kidsPer) {
        int programId = (int) classrooms.get(0).get("programId");
        RegistrationServiceClient registrationClient = new RegistrationServiceClient(programId);
        LocalDate yearStart = LocalDate.now().withMonth(9).withDayOfMonth(1);
        Map<Integer, List<Map<String, Object>>> registrationsByClassroom = new HashMap<>();

        for(Map<String, Object> classroom: classrooms) {
            List<Map<String, Object>> registrations = new LinkedList<>();
            int classroomId = (int) classroom.get("id");

            int age = getAgeFor((String) classroom.get("name"));
            String query = String.format("birthdate:[%s TO %s]", yearStart.minusYears(age).format(ISO_DATE), yearStart.minusYears(age-1).format(ISO_DATE));
            List<Integer> childIds = personClient.getPersonIds(query);
            if(childIds.size() > kidsPer)
                childIds = RandomSelector.select(childIds, kidsPer);

            Map<String, Object> reg = new HashMap<>();
            reg.put("programId", programId);
            reg.put("classroomId", classroomId);
            reg.put("schoolGrade", gradeNumberForClassroom((String) classroom.get("name")));
            for(int child: childIds){
                reg.put("enrolleeId", child);
                registrations.add(registrationClient.createRegistration(reg));
            }
            registrationsByClassroom.put(classroomId, registrations);
        }
        return registrationsByClassroom;
    }

// Attendance
    private void generateAttendance(List<Map<String, Object>> classrooms, Map<Integer, List<Map<String, Object>>> registrations) {
        int programId = (int) classrooms.get(0).get("programId");
        int sectionId = (int) classrooms.get(0).get("sectionId");
        SessionServiceClient sessionClient = new SessionServiceClient(programId, sectionId);
        AttendanceServiceClient attendanceClient = new AttendanceServiceClient(programId);
        List<Map<String, Object>> sessions = sessionClient.getPastSessions();

        for(Map<String, Object> classroom: classrooms) {
            int classroomId = (int) classroom.get("id");
            Map<String, Object> attendance = new HashMap<>();
            attendance.put("programId", programId);
            attendance.put("classroomId", classroomId);
            for(Map<String, Object> session: sessions) {
                attendance.put("sessionId", session.get("id"));
                attendance.put("enrolleeAttendance", generateEnrolleeAttendance(registrations.get(classroomId)));
                attendanceClient.recordAttendance(attendance, classroomId);
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
        String grade = gradeNumberForClassroom(gradeName);
        return grade.equals("K")? 5: Integer.parseInt(grade)+5;
    }

    private String gradeNumberForClassroom(String gradeName) {
        if(gradeName.equals("Kindergarten"))
            return "K";
        return gradeName.replaceAll("\\D", "");
    }

}
