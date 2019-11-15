package com.servantscode.fakedata.integration.serviceu;

import com.servantscode.fakedata.integration.CSVParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class ServiceuCountQC {
    public static void main(String[] args) throws IOException {
        String filePath = "c:\\Users\\gleit\\stgabriel\\events-5-14.csv";
        File importFile = new File(filePath);

//        BaseServiceClient.setUrlPrefix("https://<parish>.servantscode.org");
//        BaseServiceClient.login("user", "password");
        new ServiceuCountQC().processFile(importFile, true, 1);
    }

    public ServiceuCountQC() {
    }

    private void processFile(File importFile, boolean hasHeaders, int schedulerId) {
        int lineNumber = 0;
        String line = null;
        try {
            BufferedReader fileLines = new BufferedReader(new FileReader(importFile));

            String headers = null;
            if(hasHeaders) {
                headers = fileLines.readLine();
                System.out.println("Found header line: " + headers);
            }

            HashMap<String, AtomicInteger> eventCount = new HashMap<>(512);

            List<String> badLines = new LinkedList<>();

            while((line = fileLines.readLine()) != null) {
                String[] parsedLine = CSVParser.parseCsvLine(line);

                //Horray for fixed field structures!
                String title = parsedLine[1];
                String eventTimes = parsedLine[3];

                if(isEmpty(eventTimes)) {
                    badLines.add(line);
                    continue;
                }

                title = title.startsWith("\"")? title.substring(1): title;
                eventCount.computeIfAbsent(title, item -> new AtomicInteger(0));
                eventCount.get(title).incrementAndGet();

                lineNumber++;
            }

            List<String> titles = new ArrayList<>(eventCount.keySet());
            titles.sort(String::compareTo);

            AtomicInteger lineCount = new AtomicInteger();
            AtomicInteger bigLineCount = new AtomicInteger();
            titles.forEach(title -> {
                lineCount.getAndIncrement();
                int count = eventCount.get(title).get();
                if(count > 1) {
                    bigLineCount.getAndIncrement();
                    System.out.println(count + " | " + title);
                }
            });

            System.out.println(String.format("Identified %d unique events and %d with more than 1", lineCount.get(), bigLineCount.get()));

            if(badLines.isEmpty())
                System.out.println(String.format("Processed %d lines. 0 failures", lineNumber));
            else {
                System.err.println(String.format("Processed %d lines. %d failures", lineNumber, badLines.size()));
                if(isSet(headers))
                    System.out.println(headers);
                badLines.forEach(System.err::println);
            }

        } catch (Throwable e) {
            System.err.println("File import failed!");
            e.printStackTrace();
            System.err.println("Failing line: " + line);
        }

        System.out.println(String.format("Successfully imported %d lines.", lineNumber));
    }
}
