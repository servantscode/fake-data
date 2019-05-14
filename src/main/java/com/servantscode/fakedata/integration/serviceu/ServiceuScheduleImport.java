package com.servantscode.fakedata.integration.serviceu;

import java.io.*;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServiceuScheduleImport {
    public static void main(String[] args) throws IOException {
        File importFile = new File("c:\\Users\\gleit\\stgabriel\\events-test.csv");

        new ServiceuScheduleImport().processFile(importFile, true);
    }

    private void processFile(File importFile, boolean hasHeaders) {
        int lineNumber = 0;
        String line = null;
        try {
            BufferedReader fileLines = new BufferedReader(new FileReader(importFile));

            if(hasHeaders)
                System.out.println("Found header line: " + fileLines.readLine());

            List<String> rooms = new LinkedList<>();
            List<String> events = new LinkedList<>();
            LocalDate currentDate = null;

            while((line = fileLines.readLine()) != null) {
                String[] parsedLine = parseLine(line);
                System.out.println(line);

                lineNumber++;
            }

        } catch (IOException e) {
            System.err.println("File import failed!");
            e.printStackTrace();
            System.err.println("Failing line: " + line);
        }

        System.out.println(String.format("Successfully imported %d lines.", lineNumber));
    }
}
