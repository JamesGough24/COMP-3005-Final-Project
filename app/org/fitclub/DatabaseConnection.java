package org.fitclub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Manages database connection to postgres
public class DatabaseConnection {

    // Database credentials (Hard coded with values for my setup)
    private static final String URL = "jdbc:postgresql://localhost:5432/fitclub_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "birthday";

    // Establish and return connection to the database (or handle unsuccessful connection)
    public static Connection getConnection() throws SQLException {
        try {
            // Upload the Driver
            Class.forName("org.postgresql.Driver");
            // Make connection with specified url, username, and password
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);

            return connection;

        } catch (ClassNotFoundException e) {
            System.err.println("Postgres JDBC Driver not found.");
            throw new SQLException("Driver not found", e);
        } catch (SQLException e) {
            System.err.println("Failed to connect to database.");
            throw e;
        }
    }

    // Closes database connection
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
