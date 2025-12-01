package org.fitclub;

import java.sql.*;
import java.util.Scanner;

// Handles all Trainer-related database operations
public class TrainerOperations {

     // Operation: Set Availability
     // Allows trainer to define time windows when they're available for classes
     // Edge Case -> Overlapping time slots (trigger fires), Invalid time range (end before start)
    public static void setAvailability(Connection conn, int trainerId, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("         SET AVAILABILITY");
        System.out.println("========================================");

        try {
            // First, show current availability
            System.out.println("\nYOUR CURRENT AVAILABILITY:\n");
            displayTrainerAvailability(conn, trainerId);

            // Ask if they want to add new availability
            System.out.println("\nADD NEW AVAILABILITY SLOT");

            // Day of week
            System.out.println("\nSelect day of week:");
            System.out.println("1. Monday");
            System.out.println("2. Tuesday");
            System.out.println("3. Wednesday");
            System.out.println("4. Thursday");
            System.out.println("5. Friday");
            System.out.println("6. Saturday");
            System.out.println("7. Sunday");
            System.out.println("8. Cancel");

            System.out.print("\nEnter choice (1-8): ");
            int dayChoice = scanner.nextInt();
            // Consume newline
            scanner.nextLine();

            if (dayChoice == 8) {
                System.out.println("Cancelled.");
                return;
            }

            if (dayChoice < 1 || dayChoice > 7) {
                System.out.println("ERROR: Invalid day selection.");
                return;
            }

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            String dayOfWeek = days[dayChoice - 1];

            // Start time
            System.out.print("\nEnter start time (HH:MM in 24-hour format, e.g., 09:00): ");
            String startTimeStr = scanner.nextLine().trim();

            Time startTime;
            try {
                startTime = Time.valueOf(startTimeStr + ":00");
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Invalid time format. Use HH:MM (e.g., 09:00)");
                return;
            }

            // End time
            System.out.print("Enter end time (HH:MM in 24-hour format, e.g., 17:00): ");
            String endTimeStr = scanner.nextLine().trim();

            Time endTime;
            try {
                endTime = Time.valueOf(endTimeStr + ":00");
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Invalid time format. Use HH:MM (e.g., 17:00)");
                return;
            }

            // Validate start < end
            if (!startTime.before(endTime)) {
                System.out.println("ERROR: End time must be after start time.");
                return;
            }

            // Insert availability (trigger will check for overlaps)
            String query = "INSERT INTO TrainerAvailability (trainer_id, day_of_week, start_time, end_time) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, trainerId);
            pstmt.setString(2, dayOfWeek);
            pstmt.setTime(3, startTime);
            pstmt.setTime(4, endTime);

            pstmt.executeUpdate();
            pstmt.close();

            System.out.println("\nSUCCESS! Availability added.");
            System.out.println("Day: " + dayOfWeek);
            System.out.println("Time: " + startTimeStr + " - " + endTimeStr);

            // Show updated availability
            System.out.println("\nUPDATED AVAILABILITY:\n");
            displayTrainerAvailability(conn, trainerId);

        } catch (SQLException e) {
            if (e.getMessage().contains("overlaps with existing time slot")) {
                System.out.println("ERROR: This time slot overlaps with your existing availability.");
                System.out.println("Please choose a different time or remove the conflicting slot first.");
            } else if (e.getMessage().contains("violates check constraint")) {
                System.out.println("ERROR: Invalid time range or day of week.");
            } else {
                System.out.println("ERROR: Failed to set availability.");
                System.out.println("Details: " + e.getMessage());
            }
        }
    }

    // Helper method: Display trainer's current availability schedule
    private static void displayTrainerAvailability(Connection conn, int trainerId) {
        try {
            String query = "SELECT availability_id, day_of_week, start_time, end_time " +
                    "FROM TrainerAvailability " +
                    "WHERE trainer_id = ? " +
                    "ORDER BY " +
                    "  CASE day_of_week " +
                    "    WHEN 'Monday' THEN 1 " +
                    "    WHEN 'Tuesday' THEN 2 " +
                    "    WHEN 'Wednesday' THEN 3 " +
                    "    WHEN 'Thursday' THEN 4 " +
                    "    WHEN 'Friday' THEN 5 " +
                    "    WHEN 'Saturday' THEN 6 " +
                    "    WHEN 'Sunday' THEN 7 " +
                    "  END, " +
                    "  start_time";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, trainerId);
            ResultSet rs = pstmt.executeQuery();

            boolean hasAvailability = false;
            System.out.println("ID   | Day         | Time Range");
            System.out.println("-----+-------------+------------------");

            while (rs.next()) {
                hasAvailability = true;
                int availId = rs.getInt("availability_id");
                String day = rs.getString("day_of_week");
                Time start = rs.getTime("start_time");
                Time end = rs.getTime("end_time");

                System.out.printf("%-4d | %-11s | %s - %s\n",
                        availId, day,
                        start.toString().substring(0, 5),
                        end.toString().substring(0, 5));
            }

            if (!hasAvailability) {
                System.out.println("No availability set yet.");
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.out.println("Could not retrieve availability.");
        }
    }

     // Operation: Search Member by Name
     // Allows trainer to look up member profiles and view their progress
     // Edge Case -> No member found with that name
    public static void searchMemberByName(Connection conn, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("        SEARCH MEMBER BY NAME");
        System.out.println("========================================");

        System.out.print("\nEnter member name (first or last name): ");
        String searchTerm = scanner.nextLine().trim();

        if (searchTerm.isEmpty()) {
            System.out.println("ERROR: Please enter a search term.");
            return;
        }

        try {
            // Search for members (case-insensitive, partial match)
            String query = "SELECT member_id, first_name, last_name, email " +
                    "FROM Member " +
                    "WHERE LOWER(first_name) LIKE LOWER(?) OR LOWER(last_name) LIKE LOWER(?) " +
                    "ORDER BY last_name, first_name";

            PreparedStatement pstmt = conn.prepareStatement(query);
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            // Collect all matching members
            boolean foundMembers = false;
            System.out.println("\nSEARCH RESULTS:\n");

            while (rs.next()) {
                if (!foundMembers) {
                    foundMembers = true;
                    System.out.println("ID   | Name                     | Email");
                    System.out.println("-----+--------------------------+---------------------------");
                }

                int memberId = rs.getInt("member_id");
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                String email = rs.getString("email");

                System.out.printf("%-4d | %-24s | %s\n", memberId, fullName, email);
            }

            rs.close();
            pstmt.close();

            if (!foundMembers) {
                System.out.println("No members found matching '" + searchTerm + "'");
                return;
            }

            // Ask which member to view in detail
            System.out.print("\nEnter Member ID to view details (0 to cancel): ");
            int memberId = scanner.nextInt();
            scanner.nextLine();

            if (memberId == 0) {
                return;
            }

            // Display detailed member profile
            displayMemberProfile(conn, memberId);

        } catch (SQLException e) {
            System.out.println("ERROR: Search failed.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    // Helper method: Display detailed member profile for trainer view
    // Shows personal info, latest health metrics, and active goals
    private static void displayMemberProfile(Connection conn, int memberId) {
        System.out.println("\n========================================");
        System.out.println("          MEMBER PROFILE");
        System.out.println("========================================");

        try {
            // Get basic member info
            String memberQuery = "SELECT first_name, last_name, email, registration_date " +
                    "FROM Member WHERE member_id = ?";
            PreparedStatement memberStmt = conn.prepareStatement(memberQuery);
            memberStmt.setInt(1, memberId);
            ResultSet memberRs = memberStmt.executeQuery();

            if (!memberRs.next()) {
                System.out.println("ERROR: Member not found.");
                return;
            }

            // Personal Information
            System.out.println("\nPERSONAL INFORMATION");
            System.out.println("   Name: " + memberRs.getString("first_name") + " " + memberRs.getString("last_name"));
            System.out.println("   Email: " + memberRs.getString("email"));
            System.out.println("   Member Since: " + memberRs.getDate("registration_date"));

            memberRs.close();
            memberStmt.close();

            // Latest Health Metrics
            System.out.println("\nLATEST HEALTH METRICS");
            String metricsQuery = "SELECT date_recorded, weight, resting_heart_rate, body_fat_percentage, vo2_max " +
                    "FROM HealthMetric " +
                    "WHERE member_id = ? " +
                    "ORDER BY date_recorded DESC LIMIT 1";

            PreparedStatement metricsStmt = conn.prepareStatement(metricsQuery);
            metricsStmt.setInt(1, memberId);
            ResultSet metricsRs = metricsStmt.executeQuery();

            if (metricsRs.next()) {
                System.out.println("   Last Updated: " + metricsRs.getDate("date_recorded"));

                double weight = metricsRs.getDouble("weight");
                if (!metricsRs.wasNull()) {
                    System.out.printf("   Weight: %.1f kg\n", weight);
                }

                int heartRate = metricsRs.getInt("resting_heart_rate");
                if (!metricsRs.wasNull()) {
                    System.out.println("   Resting Heart Rate: " + heartRate + " bpm");
                }

                double bodyFat = metricsRs.getDouble("body_fat_percentage");
                if (!metricsRs.wasNull()) {
                    System.out.printf("   Body Fat: %.1f%%\n", bodyFat);
                }

                double vo2 = metricsRs.getDouble("vo2_max");
                if (!metricsRs.wasNull()) {
                    System.out.printf("   VO2 Max: %.1f ml/kg/min\n", vo2);
                }
            } else {
                System.out.println("No health metrics recorded yet.");
            }

            metricsRs.close();
            metricsStmt.close();

            // Active Fitness Goals
            System.out.println("\nACTIVE FITNESS GOALS");
            String goalsQuery = "SELECT goal_type, target_value, target_date " +
                    "FROM FitnessGoal " +
                    "WHERE member_id = ? AND status = 'Active' " +
                    "ORDER BY target_date";

            PreparedStatement goalsStmt = conn.prepareStatement(goalsQuery);
            goalsStmt.setInt(1, memberId);
            ResultSet goalsRs = goalsStmt.executeQuery();

            boolean hasGoals = false;
            while (goalsRs.next()) {
                hasGoals = true;
                String goalType = goalsRs.getString("goal_type");
                double targetValue = goalsRs.getDouble("target_value");
                Date targetDate = goalsRs.getDate("target_date");

                System.out.printf("   • %s: %.1f (Target: %s)\n", goalType, targetValue, targetDate);
            }

            if (!hasGoals) {
                System.out.println("No active goals set.");
            }

            goalsRs.close();
            goalsStmt.close();

            // Recent class attendance
            System.out.println("\nCLASS PARTICIPATION");
            String classQuery = "SELECT COUNT(*) as total_classes FROM ClassRegistration cr " +
                    "JOIN GroupClass gc ON cr.class_id = gc.class_id " +
                    "WHERE cr.member_id = ? AND gc.class_date < CURRENT_DATE";

            PreparedStatement classStmt = conn.prepareStatement(classQuery);
            classStmt.setInt(1, memberId);
            ResultSet classRs = classStmt.executeQuery();

            if (classRs.next()) {
                int totalClasses = classRs.getInt("total_classes");
                System.out.println("Total Classes Attended: " + totalClasses);
            }

            classRs.close();
            classStmt.close();

        } catch (SQLException e) {
            System.out.println("ERROR: Failed to retrieve member profile.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    // Helper method: Get trainer ID by email (for login)
    public static int getTrainerIdByEmail(Connection conn, String email) {
        try {
            String query = "SELECT trainer_id FROM Trainer WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, email);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int trainerId = rs.getInt("trainer_id");
                rs.close();
                pstmt.close();
                return trainerId;
            } else {
                rs.close();
                pstmt.close();
                return -1;  // Not found
            }

        } catch (SQLException e) {
            System.out.println("ERROR: Database error during login.");
            return -1;
        }
    }

    // Display trainer's name (used in welcome message)
    public static String getTrainerName(Connection conn, int trainerId) {
        try {
            String query = "SELECT first_name, last_name FROM Trainer WHERE trainer_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, trainerId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                rs.close();
                pstmt.close();
                return name;
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            // Ignore
        }

        return "Trainer";
    }
}