package com.servantscode.fakedata;

import org.servantscode.commons.db.DBAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

public class InitialLoginGenerator extends DBAccess {
    public static void generate() {
        try (Connection conn = getConnection();){
            if(!checkLogin(conn)) {
                createRole(conn);
                createPermission(conn);
                createFamily(conn);
                createPerson(conn);
                createLogin(conn);
            } else {
                System.out.println("Login exists. Skipping creation of initial login.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create initial login", e);
        }
    }

    private static void createRole(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO roles(name) values (?)");
        stmt.setString(1, "system");
        stmt.executeUpdate();
    }

    private static void createPermission(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO permissions(role_id, permission) values (?, ?)");
        stmt.setInt(1, 1);
        stmt.setString(2, "*");
        stmt.executeUpdate();
    }

    private static boolean checkLogin(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(1) FROM logins");
        try (ResultSet rs = stmt.executeQuery()) {
            if(rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
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
        stmt.setTimestamp(5, convert(ZonedDateTime.now()));
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
