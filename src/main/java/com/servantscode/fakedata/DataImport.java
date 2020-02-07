package com.servantscode.fakedata;

import org.servantscode.client.ApiClientFactory;
import org.servantscode.client.BaseServiceClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.servantscode.commons.StringUtils.isEmpty;

public class DataImport {

    private static final String DEFAULT_SYSTEM = "https://demo.servantscode.org";
    private static final String DEFAULT_USER = "greg@servantscode.org";

    protected static void doLogin() throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Which system? [" + DEFAULT_SYSTEM + "]");
        String system = input.readLine();
        if(isEmpty(system))
            system = DEFAULT_SYSTEM;
        if(!system.contains("//"))
            system = "https://" + system;
        if(!system.contains("."))
            system = system + ".servantscode.org";

        System.out.println(String.format("Connecting to %s.", system));

        ApiClientFactory apiFactory = ApiClientFactory.instance();
        apiFactory.setExternalPrefix(system);

        System.out.println("User? [" + DEFAULT_USER + "]");
        String user = input.readLine();
        if(isEmpty(user))
            user = DEFAULT_USER;

        System.out.println("Password? []");
        String password = input.readLine();
        if(isEmpty(user))
            throw new RuntimeException("No password supplied.");

        BaseServiceClient.login(user, password);
    }
}
