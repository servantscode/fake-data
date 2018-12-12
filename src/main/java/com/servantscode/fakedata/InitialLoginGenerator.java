package com.servantscode.fakedata;

import org.servantscode.commons.db.DBAccess;
import java.util.Date;

import java.sql.*;

public class InitialLoginGenerator extends DBAccess {
    public static void generate() {
        try (Connection conn = getConnection();){
            createFamily(conn);
            createPerson(conn);
            createLogin(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Could not create initial login", e);
        }
    }

    private static void createFamily(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO families(surname, addr_street1, addr_city, addr_state, addr_zip) values (?, ?, ?, ?, ?)");
        stmt.setString(1, "Leitheiser");
        stmt.setString(2, "849 Dalmalley Ln");
        stmt.setString(3, "Coppell");
        stmt.setString(4, "TX");
        stmt.setInt(5, 75019);
        stmt.executeUpdate();
    }

    private static void createPerson(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO people(name, email, family_id, head_of_house, member_since) values (?, ?, ?, ?, ?)");
        stmt.setString(1, "Greg Leitheiser");
        stmt.setString(2, "greg@servantscode.org");
        stmt.setInt(3, 1);
        stmt.setBoolean(4, true);
        stmt.setDate(5, convert(new Date()));
        stmt.executeUpdate();
    }

    private static void createLogin(Connection conn) throws SQLException {
        PreparedStatement stmt =
                conn.prepareStatement("INSERT INTO logins(person_id, hashed_password, role) VALUES (?, ?, ?)");

        stmt.setInt(1, 1);
        // Z@!!enHasTh1s
        stmt.setString(2, "$2a$10$ymleJy8knsspIL2c3dNnIu4c2onSsJxzOU0pBVzHs/GlSwXdFzuwO");
        stmt.setString(3, "system");
        stmt.executeUpdate();
    }
}
