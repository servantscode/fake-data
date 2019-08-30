package com.servantscode.fakedata.integration.pds;

import com.servantscode.fakedata.integration.CSVData;
import com.servantscode.fakedata.integration.CSVParser;
import com.servantscode.fakedata.integration.serviceu.ServiceuScheduleImport;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PDSImport {
    public static void main(String[] args) {
        String filePath = "C:\\Users\\gleit\\Desktop\\Parishes\\St. Mary\\sc-export.csv";
        File importFile = new File(filePath);

//        AbstractServiceClient.setUrlPrefix("https://<parish>.servantscode.org");
//        AbstractServiceClient.login("user", "password");
        new PDSImport().processFile(importFile);
    }

    private void processFile(File input) {
        CSVData personData = new CSVParser().readFile(input);

        printPersonReport(personData);
    }

    private void printPersonReport(CSVData personData) {
        System.out.println(String.format("Processed people file. Found %d records.", personData.rowData.size()));
        System.out.println(String.format("Found %d fields in records:", personData.fields.size()));
        for(String field: personData.fields)
            System.out.println(String.format("\t%s: %d", field, personData.fieldCounts.get(field).get()));
    }
}
