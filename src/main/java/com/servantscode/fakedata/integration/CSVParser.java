package com.servantscode.fakedata.integration;

import java.util.LinkedList;
import java.util.List;

public class CSVParser {
    public static String[] parseCsvLine(String line) {
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
}
