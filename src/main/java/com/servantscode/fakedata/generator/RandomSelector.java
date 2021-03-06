package com.servantscode.fakedata.generator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class RandomSelector {
    public static RandomSelector selector;

    public static Random rand = new Random();
    public Map<String, String> zipCodes;
    public List<String> streetNames;

    private RandomSelector() {
        try {
            zipCodes = ListLoader.loadMap("zip-codes.txt");
            streetNames = ListLoader.loadList("street-names.txt");
        } catch (Exception e) {
            throw new RuntimeException("Could not read data files");
        }
    }

    public static RandomSelector randomSelector() {
        if(selector == null)
            selector = new RandomSelector();

        return selector;
    }

    public static <T> T weightedSelect(Map<T, Integer> options) {
        int totalWeight = 0;
        for (Map.Entry<T, Integer> entry : options.entrySet())
            totalWeight += entry.getValue();

        int selector = rand.nextInt(totalWeight);

        for (Map.Entry<T, Integer> entry : options.entrySet()) {
            selector -= entry.getValue();
            if (selector < 0)
                return entry.getKey();
        }

        // Will never happen;
        throw new IllegalStateException(String.format("Failed random selection attempted. totalWeight:%d selector %d", totalWeight, selector));
    }

    public static <T> T select(List<T> options) {
        int optionCount = options.size();
        return options.get(rand.nextInt(optionCount));
    }

    public static <T> List<T> select(Collection<T> options, int count) {
        List<T> results = new ArrayList<>(count);
        List<T> samples = new LinkedList<>(options);
        for(int i=0;i<count;i++) {
           results.add(samples.remove(rand.nextInt(samples.size())));
        }
        return results;
    }

    public static <T> T select(Collection<T> options) {
        int optionCount = options.size();
        int selector = rand.nextInt(optionCount);
        for (T item : options) {
            if (--selector < 0)
                return item;
        }

        // Will never happen;
        throw new IllegalStateException(String.format("Failed random selection attempted. optionsCount:%d selector %d", optionCount, selector));
    }

    public static int[] randomNumbers(int max, int count) {
        if(count > max)
            throw new IllegalArgumentException();

        BitSet bits = new BitSet(max);
        while(bits.cardinality() < count)
            bits.set(rand.nextInt(max));

        int[] results = new int[count];
        int index = 0;
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i+1)) {
            results[index++] = i;
        }

        return results;
    }

    private static final List<String> phoneTypes = Arrays.asList("CELL", "WORK", "OTHER");
    public static List<Map<String, Object>> randomPhoneNumbers() {
        int count = nextInt(1, 3);
        List<Map<String, Object>> phoneNumbers = new ArrayList<>(count);
        for(int i=0; i < count; i++) {
            HashMap<String, Object> phoneNumber = new HashMap<>(8);
            phoneNumber.put("phoneNumber", randomPhoneNumber());
            phoneNumber.put("type", select(phoneTypes));
            phoneNumber.put("primary", i == 0);
            phoneNumbers.add(phoneNumber);
        }
        return phoneNumbers;
    }

    public static String randomPhoneNumber() {
        return String.format("(%03d) %03d-%04d", rand.nextInt(1000), rand.nextInt(1000), rand.nextInt(10000));
    }

    public static String randomString(int length) {
        char[] result = new char[length];
        int i = 0;
        while(i < length) {
            int nextChar = rand.nextInt(127);
            if(nextChar > 31)
                result[i++] = (char)nextChar;
        }
        return new String(result);
    }

    public static int nextInt(int low, int high) {
        return rand.nextInt(high-low) + low;
    }

    public static DayOfWeek randomDay() {
        return DayOfWeek.of(nextInt(1, 7));
    }

    public static ZonedDateTime randomTime(ZonedDateTime date) {
        ZoneId zone = date.getZone();
        return date.toLocalDate().atStartOfDay(zone).withHour(nextInt(7, 21));
    }

    public static boolean byPercentage(float v) {
        return rand.nextFloat() < v;
    }

    public String randomLocation() {
        return select(zipCodes.values());
    }
}