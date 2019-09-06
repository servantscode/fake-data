package com.servantscode.fakedata.client;

import javax.ws.rs.core.Response;
import java.util.Map;


public class NoteServiceClient extends AbstractServiceClient {

    public NoteServiceClient() { super("/rest/note"); }

    public void createNote(Map<String, Object> data) {
        Response response = post(data);

        if(response.getStatus() != 200)
            System.err.println("Failed to create note. Status: " + response.getStatus());
    }
}
