package com.servantscode.fakedata.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ListLoader {
    private static Map<String, Object> cachedData = new HashMap<>(10);

    public static List<String> loadList(String resourceName) {
        try {
            if (cachedData.containsKey(resourceName))
                return (List<String>) cachedData.get(resourceName);

            InputStream input = ListLoader.class.getClassLoader().getResourceAsStream(resourceName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            List<String> items = new LinkedList<String>();
            String item = null;
            while ((item = reader.readLine()) != null) {
                items.add(item.trim());
            }

            //Faster access.
            List<String> data = new ArrayList(items);
            cachedData.put(resourceName, data);

            return data;
        } catch (IOException e) {
            throw new RuntimeException("Could not read list: " + resourceName, e);
        }
    }

    public static Map<String, String> loadMap(String resourceName) throws IOException {
        if(cachedData.containsKey(resourceName))
            return (Map<String, String>) cachedData.get(resourceName);

        InputStream input = ListLoader.class.getClassLoader().getResourceAsStream(resourceName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        Map<String,String> items = new HashMap<>(100);
        String line = null;
        while((line = reader.readLine()) != null) {
            String[] bits = line.split(",");
            items.put(bits[0].trim(), bits[1].trim());
        }

        cachedData.put(resourceName, items);
        return items;
    }
}
