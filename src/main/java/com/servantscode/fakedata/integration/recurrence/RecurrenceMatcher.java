package com.servantscode.fakedata.integration.recurrence;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public abstract class RecurrenceMatcher {
    private String cycle;
    private LinkedList<HashMap<String, Object>> toRemove = new LinkedList<>();

    public RecurrenceMatcher(String cycle) {
        this.cycle = cycle;
    }

    public void merge(List<HashMap<String, Object>> events) {
        preSort(events);
        internalMerge(events);
        events.removeAll(toRemove);
        events.sort(Comparator.comparing(RecurrenceMatcher::startTime));
        finalMerge(events);
    }

    abstract int calculateFrequency(HashMap<String, Object> e1, HashMap<String, Object> e2);
    abstract int frequencyThreshold();
    abstract boolean isOnCycle(int frequency, HashMap<String, Object> lastEvent, HashMap<String, Object> event);
    abstract boolean matchesCycle(HashMap<String, Object> event, HashMap<String, Object> primaryEvent);
    abstract void modRecurrence(HashMap<String, Object> event, HashMap<String, Object> recur);
    abstract LocalDate calculateNextDate(int frequency, LocalDate d);
    abstract void finalMerge(List<HashMap<String, Object>> events);

    // ----- Private -----

    private void internalMerge(List<HashMap<String, Object>> events) {
        HashMap<String, Object> primaryEvent = null;
        HashMap<String, Object> lastEvent = null;
        for(HashMap<String, Object> event: events) {
            if(primaryEvent == null || !sameTime(event, primaryEvent) || !matchesCycle(event, primaryEvent) ||
                    recurrence(event) != null || recurrence(lastEvent) != null || !resourcesMatch(primaryEvent, event)) {
                primaryEvent = event; //Reset
            } else if(recurrence(primaryEvent) == null) {
                int frequency = calculateFrequency(lastEvent, event);
                if(frequency > 0 && frequency <= frequencyThreshold()) {
                    primaryEvent.put("recurrence", createRecurrence(lastEvent, event));
                    toRemove.add(event);
                } else {
                    primaryEvent = event; //Reset
                }
            } else if(isOnCycle(recurringFrequency(recurrence(primaryEvent)), lastEvent, event)) {
                checkForExceptions(primaryEvent, lastEvent, event);
                recurrence(primaryEvent).put("endDate", startTime(event).toLocalDate());
                toRemove.add(event);
            } else {
                primaryEvent = event; //Reset
            }

            lastEvent = event;
        }
    }

    private boolean resourcesMatch(HashMap<String, Object> e1, HashMap<String, Object> e2) {
        List<HashMap<String, Object>> res1 = (List<HashMap<String, Object>>)e1.get("reservations");
        List<HashMap<String, Object>> res2 = (List<HashMap<String, Object>>)e2.get("reservations");
        if(res1.size() != res2.size())
            return false;

        for(HashMap<String, Object> testRes: res1) {
            boolean hasMatch = res2.stream().anyMatch(res ->
                    res.get("resourceType").equals(testRes.get("resourceType"))
                    && res.get("resourceId").equals(testRes.get("resourceId"))
                    && sameTime(startTime(res), startTime(testRes))
                    && sameTime(endTime(res), endTime(testRes))
                );

            if(!hasMatch)
                return false;
        }
        return true;
    }

    private static boolean sameTime(ZonedDateTime d1, ZonedDateTime d2) {
        return d1.toLocalTime().equals(d2.toLocalTime());
    }

    void preSort(List<HashMap<String, Object>> events) {
        events.sort(Comparator.comparing(d -> startTime(d)));
    }

    private void checkForExceptions(HashMap<String, Object> primaryEvent, HashMap<String, Object> lastEvent, HashMap<String, Object> event) {
        HashMap<String, Object> recurrence = recurrence(primaryEvent);
        LocalDate nextDate = calculateNextDate(recurringFrequency(recurrence), startTime(lastEvent).toLocalDate());
        LocalDate targetDate = startTime(event).toLocalDate();
        while(nextDate.isBefore(targetDate)) {
            ((LinkedList<LocalDate>) recurrence.get("exceptionDates")).add(nextDate);
            nextDate = calculateNextDate(recurringFrequency(recurrence), nextDate);
        }
    }

    private HashMap<String, Object> createRecurrence(HashMap<String, Object> lastEvent, HashMap<String, Object> event) {
        HashMap<String, Object> recur = new HashMap<>(8);
        recur.put("cycle", cycle);
        recur.put("frequency", calculateFrequency(lastEvent, event));
        recur.put("endDate", startTime(event).toLocalDate());
        recur.put("exceptionDates", new LinkedList<LocalDate>());

        modRecurrence(event, recur);
        return recur;
    }

    private static boolean sameTime(HashMap<String, Object> e1, HashMap<String, Object> e2) {
        return sameTime(startTime(e1), startTime(e2)) && sameTime(endTime(e1), endTime(e2));
    }

    private static HashMap<String, Object> recurrence(HashMap<String, Object> event) {
        return event.containsKey("recurrence")? (HashMap<String, Object>) event.get("recurrence"): null;
    }

    private Integer recurringFrequency(HashMap<String, Object> recurrence) {
        return recurrence == null? 0: (Integer) recurrence.get("frequency");
    }

    private static ZonedDateTime startTime(HashMap<String, Object> event) {
        return (ZonedDateTime)event.get("startTime");
    }

    private static ZonedDateTime endTime(HashMap<String, Object> event) {
        return (ZonedDateTime)event.get("endTime");
    }

    public static class WeeklyMatcher extends RecurrenceMatcher {
        public WeeklyMatcher() {
            super("WEEKLY");
        }

        void preSort(List<HashMap<String, Object>> events) {
            events.sort(Comparator.comparingInt(event -> compressSortCriteria(startTime(event))));
        }

        private int compressSortCriteria(ZonedDateTime d) {
            return d.getDayOfWeek().getValue()*10000+d.getHour()*100+d.getMinute();
        }

        boolean matchesCycle(HashMap<String, Object> event, HashMap<String, Object> primaryEvent) {
            return startTime(event).getDayOfWeek() == startTime(primaryEvent).getDayOfWeek();
        }

        int frequencyThreshold() {
            return 4;
        }

        boolean isOnCycle(int frequency, HashMap<String, Object> lastEvent, HashMap<String, Object> event) {
            return frequency > 0 && diffDays(lastEvent, event) % (frequency*7) == 0;
        }

        int calculateFrequency(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            return diffDays(e1, e2)/7;
        }

        private int diffDays(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            int diffDays = startTime(e2).getDayOfYear() - startTime(e1).getDayOfYear();
            return (diffDays >= 0)?
                    diffDays:
                    diffDays + (startTime(e1).toLocalDate().isLeapYear()? 366: 365);
        }

        LocalDate calculateNextDate(int frequency, LocalDate d) {
            return d.plusDays(frequency*7);
        }

        void modRecurrence(HashMap<String, Object> event, HashMap<String, Object> recur) {
            recur.put("weeklyDays", new LinkedList<>(Collections.singletonList(startTime(event).getDayOfWeek().toString())));
        }

        void finalMerge(List<HashMap<String, Object>> events) {
            HashMap<String, Object> lastEvent = events.get(0);
            for(int i=1; i< events.size();) {
                HashMap<String, Object> event = events.get(i);
                if(overlaps(lastEvent, event)) {
                    mergeWeekDays(lastEvent, event);
                    events.remove(event);
                } else {
                    lastEvent = event;
                    i++;
                }
            }
        }

        // ----- Private -----
        private boolean overlaps(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            if(recurrence(e1) == null || recurrence(e2) == null || !sameTime(startTime(e1), startTime(e2)) ||
                    !recurrence(e1).get("frequency").equals(recurrence(e2).get("frequency")) ||
                    startTime(e1).plusDays(7).getDayOfYear() <= startTime(e2).getDayOfYear())
                return false;
            int end1 = ((LocalDate)recurrence(e1).get("endDate")).plusDays(7).getDayOfYear();
            int end2 = ((LocalDate)recurrence(e2).get("endDate")).getDayOfYear();
            return end1 > end2 && end1 - end2 < 7;
        }

        private void mergeWeekDays(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            HashMap<String, Object> recur = recurrence(e1);
            HashMap<String, Object> recur2 = recurrence(e2);
            recur.put("endDate", recur2.get("endDate"));
            ((List<String>)recur.get("weeklyDays")).addAll((List<String>)recur2.get("weeklyDays"));
            ((List<LocalDate>)recur.get("exceptionDates")).addAll((List<LocalDate>)recur2.get("exceptionDates"));
        }
    }

    public static class DailyMatcher extends RecurrenceMatcher {
        public DailyMatcher() {
            super("DAILY");
        }

        boolean matchesCycle(HashMap<String, Object> event, HashMap<String, Object> primaryEvent) {
            return true;
        }

        int frequencyThreshold() {
            return 2;
        }

        boolean isOnCycle(int frequency, HashMap<String, Object> lastEvent, HashMap<String, Object> event) {
            int diffDays = diffDays(lastEvent, event);
            return frequency > 0 &&  diffDays % frequency == 0 && diffDays/frequency < 3;
        }

        int calculateFrequency(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            return diffDays(e1, e2);
        }

        private int diffDays(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            int diffDays = startTime(e2).getDayOfYear() - startTime(e1).getDayOfYear();
            return (diffDays >= 0)?
                    diffDays:
                    diffDays + (startTime(e1).toLocalDate().isLeapYear()? 366: 365);
        }

        LocalDate calculateNextDate(int frequency, LocalDate d) {
            return d.plusDays(frequency);
        }

        void modRecurrence(HashMap<String, Object> event, HashMap<String, Object> recur) {
        }

        void finalMerge(List<HashMap<String, Object>> events) {}
    }

    public static class WeekDayOfMonthMathcer extends RecurrenceMatcher {
        public WeekDayOfMonthMathcer() {
            super("WEEKDAY_OF_MONTH");
        }

        void preSort(List<HashMap<String, Object>> events) {
            events.sort(Comparator.comparingInt(e -> sortCriteria(startTime(e))));
        }

        int sortCriteria(ZonedDateTime d) {
            return d.getDayOfWeek().getValue()*100 + weekOfMonth(d);
        }

        boolean matchesCycle(HashMap<String, Object> event, HashMap<String, Object> primaryEvent) {
            ZonedDateTime primaryDate = startTime(primaryEvent);
            ZonedDateTime eventDate = startTime(event);
            return primaryDate.getDayOfWeek() == eventDate.getDayOfWeek() &&
                   weekOfMonth(primaryDate) == weekOfMonth(eventDate);
        }

        int frequencyThreshold() {
            return 6;
        }

        boolean isOnCycle(int frequency, HashMap<String, Object> lastEvent, HashMap<String, Object> event) {
            if( frequency <= 0)
                return false;

            ZonedDateTime lastDate = startTime(lastEvent);
            ZonedDateTime eventDate = startTime(event);
            return lastDate.getDayOfWeek() == eventDate.getDayOfWeek() &&
                   weekOfMonth(lastDate) == weekOfMonth(eventDate) &&
                   diffMonths(lastEvent, event) % frequency == 0;
        }

        int calculateFrequency(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            return diffMonths(e1, e2);
        }

        private int diffMonths(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            int months = startTime(e2).getMonthValue() - startTime(e1).getMonthValue();
            return (months >= 0)? months: months + 12;
        }

        LocalDate calculateNextDate(int frequency, LocalDate d) {
            return d.plusMonths(frequency).with(TemporalAdjusters.dayOfWeekInMonth(weekOfMonth(d), d.getDayOfWeek()));
        }

        void modRecurrence(HashMap<String, Object> event, HashMap<String, Object> recur) {
        }

        void finalMerge(List<HashMap<String, Object>> events) {}
        //----- Private -----
        private int weekOfMonth(ZonedDateTime date) {
            return (date.getDayOfMonth()-1)/7+1;
        }

        private int weekOfMonth(LocalDate date) {
            return (date.getDayOfMonth()-1)/7+1;
        }
    }

    public static class MonthMatcher extends RecurrenceMatcher {
        public MonthMatcher() {
            super("DAY_OF_MONTH");
        }

        void preSort(List<HashMap<String, Object>> events) {
            events.sort(Comparator.comparingInt(e -> sortCriteria(startTime(e))));
        }

        int sortCriteria(ZonedDateTime d) {
            return  d.getDayOfMonth() * 1000000 + d.getYear() * 100 + d.getMonthValue();
        }

        boolean matchesCycle(HashMap<String, Object> event, HashMap<String, Object> primaryEvent) {
            ZonedDateTime primaryDate = startTime(primaryEvent);
            ZonedDateTime eventDate = startTime(event);
            return primaryDate.getDayOfMonth() == eventDate.getDayOfMonth();
        }

        int frequencyThreshold() {
            return 6;
        }

        boolean isOnCycle(int frequency, HashMap<String, Object> lastEvent, HashMap<String, Object> event) {
            if( frequency <= 0)
                return false;

            ZonedDateTime lastDate = startTime(lastEvent);
            ZonedDateTime eventDate = startTime(event);
            return lastDate.getDayOfMonth() == eventDate.getDayOfMonth() &&
                    diffMonths(lastEvent, event) % frequency == 0;

        }

        int calculateFrequency(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            return diffMonths(e1, e2);
        }

        private int diffMonths(HashMap<String, Object> e1, HashMap<String, Object> e2) {
            int months = startTime(e2).getMonthValue() - startTime(e1).getMonthValue();
            return (months >= 0)? months: months + 12;
        }

        LocalDate calculateNextDate(int frequency, LocalDate d) {
            return d.plusMonths(frequency);
        }

        void modRecurrence(HashMap<String, Object> event, HashMap<String, Object> recur) {
        }

        void finalMerge(List<HashMap<String, Object>> events) {}
    }
}
