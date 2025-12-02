package org.fitclub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

// Main entry point for Health and Fitness Club Management System
public class Main {

    // Global variables to track current user session
    private static int currentMemberId = -1;
    private static int currentTrainerId = -1;
    private static int currentAdminId = -1;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Connection conn = null;

        try {
            // Establish database connection
            conn = DatabaseConnection.getConnection();

            // Main application loop
            boolean running = true;
            while (running) {
                displayMainMenu();

                int choice = getMenuChoice(scanner, 1, 5);

                switch (choice) {
                    case 1:
                        handleMemberLogin(conn, scanner);
                        break;
                    case 2:
                        handleTrainerLogin(conn, scanner);
                        break;
                    case 3:
                        handleAdminLogin(conn, scanner);
                        break;
                    case 4:
                        // User Registration (before login)
                        MemberOperations.registerMember(conn, scanner);
                        break;
                    case 5:
                        running = false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            System.err.println("Please ensure PostgreSQL is running and credentials are correct.");
        } finally {
            // Close database connection
            if (conn != null) {
                DatabaseConnection.closeConnection(conn);
            }
            scanner.close();
        }
    }

    // Display the main menu
    private static void displayMainMenu() {
        System.out.println("\n========================================");
        System.out.println("  HEALTH & FITNESS CLUB MANAGEMENT");
        System.out.println("========================================");
        System.out.println("1. Sign in as Member");
        System.out.println("2. Sign in as Trainer");
        System.out.println("3. Sign in as Admin");
        System.out.println("4. Sign up as New Member");
        System.out.println("5. Exit");
        System.out.println("========================================");
    }

    // Handle Member login and menu
    private static void handleMemberLogin(Connection conn, Scanner scanner) {
        System.out.println("\n=== MEMBER LOGIN ===");
        System.out.print("Enter your email: ");
        String email = scanner.nextLine().trim();

        // Verify email exists and get member_id
        currentMemberId = MemberOperations.getMemberIdByEmail(conn, email);

        if (currentMemberId == -1) {
            System.out.println("No Member found with that email.");
            return;
        }

        // Get member name for welcome message
        String memberName = MemberOperations.getMemberName(conn, currentMemberId);
        System.out.println("Welcome back, " + memberName + "!");

        // Member menu loop
        boolean loggedIn = true;
        while (loggedIn) {
            displayMemberMenu();

            int choice = getMenuChoice(scanner, 1, 4);

            switch (choice) {
                case 1:
                    handleUpdateProfile(conn, scanner);
                    break;
                case 2:
                    MemberOperations.viewDashboard(conn, currentMemberId);
                    break;
                case 3:
                    MemberOperations.registerForGroupClass(conn, currentMemberId, scanner);
                    break;
                case 4:
                    loggedIn = false;
                    currentMemberId = -1;
                    System.out.println("Signed out successfully.");
                    break;
            }
        }
    }

    // Display Member menu
    private static void displayMemberMenu() {
        System.out.println("\n========================================");
        System.out.println("          MEMBER MENU");
        System.out.println("========================================");
        System.out.println("1. Update Profile");
        System.out.println("2. View Dashboard");
        System.out.println("3. Register for Group Class");
        System.out.println("4. Sign Out");
        System.out.println("========================================");
    }

    // Handle Update Profile submenu
    private static void handleUpdateProfile(Connection conn, Scanner scanner) {
        System.out.println("\n=== UPDATE PROFILE ===");
        System.out.println("1. Update Personal Information");
        System.out.println("2. Create New Fitness Goal");
        System.out.println("3. Log New Health Metric");
        System.out.println("4. Back to Menu");
        System.out.println("========================");

        int choice = getMenuChoice(scanner, 1, 4);

        switch (choice) {
            case 1:
                MemberOperations.updatePersonalInfo(conn, currentMemberId, scanner);
                break;
            case 2:
                MemberOperations.createFitnessGoal(conn, currentMemberId, scanner);
                break;
            case 3:
                MemberOperations.logHealthMetric(conn, currentMemberId, scanner);
                break;
            case 4:
                // Back to menu
                break;
        }
    }

    // Handle Trainer login and menu
    private static void handleTrainerLogin(Connection conn, Scanner scanner) {
        System.out.println("\n=== TRAINER LOGIN ===");
        System.out.print("Enter your email: ");
        String email = scanner.nextLine().trim();

        // Verify email exists and get trainer_id
        currentTrainerId = TrainerOperations.getTrainerIdByEmail(conn, email);

        if (currentTrainerId == -1) {
            System.out.println("No Trainer found with that email.");
            return;
        }

        // Get trainer name for welcome message
        String trainerName = TrainerOperations.getTrainerName(conn, currentTrainerId);
        System.out.println("Welcome back, " + trainerName + "!");

        // Trainer menu loop
        boolean loggedIn = true;
        while (loggedIn) {
            displayTrainerMenu();

            int choice = getMenuChoice(scanner, 1, 3);

            switch (choice) {
                case 1:
                    TrainerOperations.setAvailability(conn, currentTrainerId, scanner);
                    break;
                case 2:
                    TrainerOperations.searchMemberByName(conn, scanner);
                    break;
                case 3:
                    loggedIn = false;
                    currentTrainerId = -1;
                    System.out.println("Signed out successfully.");
                    break;
            }
        }
    }

    // Display Trainer menu
    private static void displayTrainerMenu() {
        System.out.println("\n========================================");
        System.out.println("          TRAINER MENU");
        System.out.println("========================================");
        System.out.println("1. Set Availability");
        System.out.println("2. Search Member by Name");
        System.out.println("3. Sign Out");
        System.out.println("========================================");
    }

    // Handle Admin login and menu
    private static void handleAdminLogin(Connection conn, Scanner scanner) {
        System.out.println("\n=== ADMIN LOGIN ===");
        System.out.print("Enter your email: ");
        String email = scanner.nextLine().trim();

        // Verify email exists and get admin_id
        currentAdminId = AdminOperations.getAdminIdByEmail(conn, email);

        if (currentAdminId == -1) {
            System.out.println("No Admin found with that email.");
            return;
        }

        // Get admin name for welcome message
        String adminName = AdminOperations.getAdminName(conn, currentAdminId);
        System.out.println("Welcome back, " + adminName + "!");

        // Admin menu loop
        boolean loggedIn = true;
        while (loggedIn) {
            displayAdminMenu();

            int choice = getMenuChoice(scanner, 1, 2);

            switch (choice) {
                case 1:
                    AdminOperations.createGroupClass(conn, scanner);
                    break;
                case 2:
                    loggedIn = false;
                    currentAdminId = -1;
                    System.out.println("Signed out successfully.");
                    break;
            }
        }
    }

    // Display Admin menu
    private static void displayAdminMenu() {
        System.out.println("\n========================================");
        System.out.println("           ADMIN MENU");
        System.out.println("========================================");
        System.out.println("1. Create New Group Class");
        System.out.println("2. Sign Out");
        System.out.println("========================================");
    }

    // Get valid menu choice from user
    private static int getMenuChoice(Scanner scanner, int min, int max) {
        int choice = -1;
        boolean valid = false;

        while (!valid) {
            System.out.print("\nEnter your choice (" + min + "-" + max + "): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                // Consume newline
                scanner.nextLine();

                if (choice >= min && choice <= max) {
                    valid = true;
                } else {
                    System.out.println("Invalid choice. Please enter a number between " + min + " and " + max + ".");
                }
            } else {
                // Clear invalid input
                scanner.nextLine();
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        return choice;
    }
}
