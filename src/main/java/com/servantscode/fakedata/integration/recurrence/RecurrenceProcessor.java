package com.servantscode.fakedata.integration.recurrence;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ofPattern;

public class RecurrenceProcessor {

    public List<HashMap<String, Object>> identifyRecurrences(List<HashMap<String, Object>> events) {
        HashMap<String, List<HashMap<String, Object>>> likeEvents = new HashMap<>(128);
        for (HashMap<String, Object> event : events) {
            String title = (String) event.get("title");// + " - " + (String) event.get("description");
            likeEvents.computeIfAbsent(title, str -> new LinkedList<>());
            likeEvents.get(title).add(event);
        }

        List<List<HashMap<String, Object>>> finalEvents = likeEvents.values().stream().map(likeEvent -> {
            if(likeEvent.size() == 1)
                return likeEvent;

            List<HashMap<String, Object>> mergedEvents = tryFit(new RecurrenceMatcher.DailyMatcher(), likeEvent, null);
            mergedEvents = tryFit(new RecurrenceMatcher.WeeklyMatcher(), likeEvent, mergedEvents);
            mergedEvents = tryFit(new RecurrenceMatcher.MonthMatcher(), likeEvent, mergedEvents);
            mergedEvents = tryFit(new RecurrenceMatcher.WeekDayOfMonthMathcer(), likeEvent, mergedEvents);
            return mergedEvents;
        }).collect(Collectors.toList());

        printReport(finalEvents);

        return flatten(finalEvents);
    }

    private List<HashMap<String, Object>> flatten(List<List<HashMap<String, Object>>> events) {
        List<HashMap<String, Object>> flattened = new LinkedList<>();
        events.forEach(flattened::addAll);
        return flattened;
    }

    private List<HashMap<String, Object>> tryFit(RecurrenceMatcher matcher,
                                                 List<HashMap<String, Object>> likeEvent,
                                                 List<HashMap<String, Object>> prevBest) {

        List<HashMap<String, Object>> cloned = likeEvent.stream().map(HashMap::new).collect(Collectors.toList());
        matcher.merge(cloned);
        return prevBest == null || cloned.size() < prevBest.size()? cloned: prevBest;
    }

    private void printReport(Collection<List<HashMap<String, Object>>> likeEvents) {
        likeEvents.forEach(list -> {
            list.forEach(event -> {
                System.out.print(String.format("%s, %s, %s, %s",
                        event.get("title"), event.get("description"), formatDate(startTime(event)),
                        formatTimeRange(startTime(event), (ZonedDateTime)event.get("endTime"))));
                ((List<HashMap<String, Object>>)event.get("reservations")).forEach(res -> {
                    System.out.print(String.format(", %s, %d", res.get("resourceType"), (Integer)res.get("resourceId")));
                });

                if(event.containsKey("recurrence")) {
                    HashMap<String, Object> recur = (HashMap<String, Object>) event.get("recurrence");
                    System.out.print(String.format("\n\tRecurs: Every %d %s ", (Integer)recur.get("frequency"), recur.get("cycle")));
                    if(recur.get("cycle") == "WEEKLY")
                        System.out.print(String.format("On: %s ", recur.get("weeklyDays")));
                    System.out.print(String.format("Until: %s", formatDate((LocalDate)recur.get("endDate"))));
                    List<LocalDate> exceptions = ((LinkedList<LocalDate>)recur.get("exceptionDates"));
                    if(!exceptions.isEmpty())
                        System.out.print(" Except: " + exceptions.stream().map(this::formatDate).collect(Collectors.joining(", ")));
                }
                System.out.println();
            });
            System.out.println();
        });
    }

    private ZonedDateTime startTime(HashMap<String, Object> event) {
        return (ZonedDateTime)event.get("startTime");
    }

    private ZonedDateTime endTime(HashMap<String, Object> event) {
        return (ZonedDateTime)event.get("endTime");
    }

    private String formatTimeRange(ZonedDateTime startTime, ZonedDateTime endTime) {
        return startTime.format(ofPattern("kk:mm")) + " - " + endTime.format(ofPattern("kk:mm"));
    }

    private String formatDate(ZonedDateTime startTime) {
        return startTime.format(ofPattern("EEE LLL dd yy"));
    }

    private String formatDate(LocalDate date) {
        return date.format(ofPattern("LLL dd yy"));
    }
}
