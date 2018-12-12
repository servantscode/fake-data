package com.servantscode.fakedata;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
}