package com.servantscode.fakedata.integration.serviceu;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class ServiceuQC {
    public static void main(String[] args) throws IOException {
        String filePath = "c:\\Users\\gleit\\stgabriel\\raw-schedule.txt";
        String filePath2 = "c:\\Users\\gleit\\stgabriel\\processed2-schedule.txt";

        BufferedReader rawLines = new BufferedReader(new FileReader(filePath));
        BufferedReader processedLines = new BufferedReader(new FileReader(filePath2));

        String skipTo = "Baccalaureate Mass";
        boolean skip = true;

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        List<String> block;
        do {
            block = readBlock(rawLines);
            List<String> block2 = readBlock(processedLines);

            if(skip) {
                if (block.get(0).startsWith(skipTo))
                    skip = false;
                else
                    continue;
            }

            if(block.size() != 1 && (block.size() != block2.size() || !block.equals(block2))) {
                block.forEach(System.out::println);
                System.out.println();
                block2.forEach(System.out::println);
                System.out.println("\n===========\n");

                in.readLine();
            }
    } while(block.size() > 0);

    }

    private static List<String> readBlock(BufferedReader rawLines) throws IOException {
        List<String> lines = new LinkedList<>();
        String line = null;
        while((line = rawLines.readLine()) != null) {
            if(line.trim().equals("")) {
                break;
            } else {
                lines.add(line);
            }
        }
        return lines;
    }
}
