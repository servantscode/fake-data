package com.servantscode.fakedata.integration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class CSVParser {
    public CSVParser() {}

    public CSVData readFiles(List<File> importFiles) {
        CSVData data = new CSVData();
        importFiles.forEach(f -> doRead(data, f));
        return data;
    }

    public CSVData readFile(File importFile) {
        CSVData data = new CSVData();
        doRead(data, importFile);
        return data;
    }

    private void doRead(CSVData data, File importFile) {
        int lineNumber = 1;
        try {
            BufferedReader fileLines = new BufferedReader(new FileReader(importFile));
            List<String> badLines = new LinkedList<>();

            String headers = fileLines.readLine();
            processHeaders(data, headers);

            String line = null;
            while((line = fileLines.readLine()) != null) {
                lineNumber++;

                try {
                    HashMap<String, String> entry = processLine(data, line);
                    if(!entry.isEmpty())
                        data.rowData.add(entry);
                } catch (Exception e) {
                    System.err.println("Failed to parse line " + lineNumber + ": " + e.getMessage());
                    badLines.add(line);
                }
            }

            System.out.println(String.format("Processed %s -- %d lines. %d failures", importFile.getCanonicalPath(), lineNumber, badLines.size()));

            if(!badLines.isEmpty()) {
                System.out.println(headers);
                badLines.forEach(System.err::println);
            }
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Processing %s failed on line: %d", importFile.getPath(), lineNumber), e);
        }
    }

    private void processHeaders(CSVData data, String headers) {
        System.out.println("Found header line: " + headers);

        String[] columns = parseCsvLine(headers);
        if(!data.fields.isEmpty()) {
            Iterator<String> fieldNames = data.fields.iterator();
            for (String column : columns) {
                if(!stripQuotes(column).equals(fieldNames.next()))
                    throw new IllegalArgumentException("Input files do not have same headers");
            }
        } else {
            for (String column : columns) {
                String columnName = stripQuotes(column);
                data.fields.add(columnName);
                data.fieldCounts.put(columnName, new AtomicInteger());
            }
        }
    }

    private HashMap<String, String> processLine(CSVData data, String line) {
        String[] parsedLine = CSVParser.parseCsvLine(line);

        HashMap<String, String> entry = new HashMap<>(data.fields.size());

        Iterator<String> fieldNames = data.fields.iterator();
        for(String column: parsedLine) {
            String field = fieldNames.next();
            String fieldData = stripQuotes(column);

            if(isSet(fieldData)) {
                entry.put(field, fieldData);
                data.fieldCounts.get(field).incrementAndGet();
            }
        }
        return entry;
    }

    private String stripQuotes(String input) {
        return (input.length() > 1 && input.startsWith("\"") && input.endsWith("\""))?
            input.substring(1, input.length()-1):
            input;
    }

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
