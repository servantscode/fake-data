package com.servantscode.fakedata.integration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CSVData {
    public LinkedList<String> fields = new LinkedList<>();
    public HashMap<String, AtomicInteger> fieldCounts = new HashMap<>(1024);
    public List<HashMap<String, String>> rowData = new LinkedList<>();
}
