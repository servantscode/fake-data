package com.servantscode.fakedata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FakeDataGenerator {
    public static void main(String[] args) throws IOException {
//        InitialLoginGenerator.generate();
        FamilyGenerator.generate(100);
        MinistryGenerator.generate(1000);
    }
}
