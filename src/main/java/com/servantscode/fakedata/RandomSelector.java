package com.servantscode.fakedata;

import java.time.LocalDate;
import java.util.*;

public class RandomSelector {
    public static Random rand = new Random();

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

    public static <T> List<T> select(List<T> options, int count) {
        List<T> results = new ArrayList<>(count);
        List<T> samples = new LinkedList<>(options);
        for(int i=0;i<count;i++) {
           results.add(samples.remove(rand.nextInt(samples.size())));
        }
        return results;
    }

    public static <T> T select(Set<T> options) {
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

    public static LocalDate randomDate() {
        return LocalDate.now().withDayOfYear(rand.nextInt(365));
    }
}