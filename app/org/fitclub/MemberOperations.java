package org.fitclub;

import java.sql.*;
import java.util.Scanner;

// Handles all Member-related database operations
public class MemberOperations {

    // Operation: User Registration
    // Registers a new member in the system
    // Edge Case -> Try to sign up with a duplicate email (UNIQUE constraint violation)
    public static void registerMember(Connection connection, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("        NEW MEMBER REGISTRATION");
        System.out.println("========================================");

        try {
            // Collect member information
            System.out.print("First Name: ");
            String firstName = scanner.nextLine().trim();

            System.out.print("Last Name: ");
            String lastName = scanner.nextLine().trim();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            // Validate required fields
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                System.out.println("ERROR: First name, last name, and email are required.");
                return;
            }

            // Insert new member
            String query = "INSERT INTO Member (first_name, last_name, email) " +
                    "VALUES (?, ?, ?) RETURNING member_id";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, email);

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                int memberId = rs.getInt("member_id");
                System.out.println("\nSUCCESS: Member registered successfully.");
                System.out.println("   Member ID: " + memberId);
                System.out.println("   Name: " + firstName + " " + lastName);
                System.out.println("   Email: " + email);
                System.out.println("\n   You can now sign in using your email!");
            }

            rs.close();
            statement.close();

        } catch (SQLException e) {
            // Handle specific error cases
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                System.out.println("ERROR: This email is already registered.");
            } else if (e.getMessage().contains("violates not-null constraint")) {
                System.out.println("ERROR: Missing required field.");
            } else {
                System.out.println("ERROR: Registration failed.");
                System.out.println("Details: " + e.getMessage());
            }
        }
    }

    // Helper Function for View Dashboard operation
    private static void displayUpcomingClasses(Connection connection, int memberId) {
        try {
            String query = "SELECT gc.class_name, gc.class_date, gc.start_time, gc.end_time, " +
                    "r.room_name, t.first_name || ' ' || t.last_name as trainer_name " +
                    "FROM ClassRegistration cr " +
                    "JOIN GroupClass gc ON cr.class_id = gc.class_id " +
                    "JOIN Room r ON gc.room_id = r.room_id " +
                    "JOIN Trainer t ON gc.trainer_id = t.trainer_id " +
                    "WHERE cr.member_id = ? AND gc.class_date >= CURRENT_DATE " +
                    "ORDER BY gc.class_date, gc.start_time";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, memberId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String className = rs.getString("class_name");
                Date classDate = rs.getDate("class_date");
                Time startTime = rs.getTime("start_time");
                Time endTime = rs.getTime("end_time");
                String roomName = rs.getString("room_name");
                String trainerName = rs.getString("trainer_name");

                System.out.printf("   • %s | %s | %s-%s | %s | Trainer: %s\n",
                        className, classDate, startTime.toString().substring(0, 5),
                        endTime.toString().substring(0, 5), roomName, trainerName);
            }

            rs.close();
            statement.close();

        } catch (SQLException e) {
            System.out.println("Could not retrieve class details.");
        }
    }

    // Operation: View Dashboard
    // Displays  member's dashboard with latest metrics, goals, and class info
    // Edge Case -> Member has no health metrics or goals (shows NULL/0 values)
    public static void viewDashboard(Connection connection, int memberId) {
        System.out.println("\n========================================");
        System.out.println("           MEMBER DASHBOARD");
        System.out.println("========================================");

        try {
            // Query the MemberDashboard view
            String query = "SELECT * FROM MemberDashboard WHERE member_id = ?";
            // Pass in the memberId of the current Member
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, memberId);

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                // Personal Information
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String email = rs.getString("email");

                System.out.println("\nPERSONAL INFORMATION");
                System.out.println("   Name: " + firstName + " " + lastName);
                System.out.println("   Email: " + email);

                // Latest Health Metrics
                System.out.println("\nLATEST HEALTH METRICS");
                Double latestWeight = rs.getDouble("latest_weight");
                Integer latestHeartRate = rs.getInt("latest_heart_rate");
                Double latestBodyFat = rs.getDouble("latest_body_fat");
                Date latestMetricDate = rs.getDate("latest_metric_date");

                if (rs.wasNull() || latestMetricDate == null) {
                    System.out.println("No health metrics recorded yet.");
                } else {
                    System.out.println("   Last Updated: " + latestMetricDate);
                    if (latestWeight > 0) {
                        System.out.printf("   Weight: %.1f kg\n", latestWeight);
                    }
                    if (latestHeartRate > 0) {
                        System.out.println("   Resting Heart Rate: " + latestHeartRate + " bpm");
                    }
                    if (latestBodyFat > 0) {
                        System.out.printf("   Body Fat: %.1f%%\n", latestBodyFat);
                    }
                }

                // Fitness Goals
                System.out.println("\nFITNESS GOALS");
                int activeGoalsCount = rs.getInt("active_goals_count");
                if (activeGoalsCount == 0) {
                    System.out.println("No active fitness goals set.");
                } else {
                    System.out.println("Active Goals: " + activeGoalsCount);
                }

                // Class Participation
                System.out.println("\nCLASS PARTICIPATION");
                int pastClassesCount = rs.getInt("past_classes_count");
                int upcomingClassesCount = rs.getInt("upcoming_classes_count");

                System.out.println("Past Classes Attended: " + pastClassesCount);
                System.out.println("Upcoming Classes: " + upcomingClassesCount);

                if (upcomingClassesCount > 0) {
                    System.out.println("\nUPCOMING CLASS SCHEDULE:");
                    displayUpcomingClasses(connection, memberId);
                }

            } else {
                System.out.println("ERROR: Member not found.");
            }

            rs.close();
            statement.close();

        } catch (SQLException e) {
            System.out.println("ERROR: Failed to retrieve dashboard.");
            System.out.println("Details: " + e.getMessage());
        }

        System.out.println("\n========================================");
    }

    // Helper method: Get member ID by email (for login)
    public static int getMemberIdByEmail(Connection conn, String email) {
        try {
            String query = "SELECT member_id FROM Member WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, email);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int memberId = rs.getInt("member_id");
                rs.close();
                pstmt.close();
                return memberId;
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

    // Display member's name (used in welcome message)
    public static String getMemberName(Connection conn, int memberId) {
        try {
            String query = "SELECT first_name, last_name FROM Member WHERE member_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, memberId);

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

        return "Member";
    }

    // Operation: Update member's account information
    public static void updatePersonalInfo(Connection conn, int memberId, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("      UPDATE PERSONAL INFORMATION");
        System.out.println("========================================");

        try {
            // First, display current information
            String selectQuery = "SELECT first_name, last_name, email " +
                    "FROM Member WHERE member_id = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
            selectStmt.setInt(1, memberId);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("ERROR: Member not found.");
                return;
            }

            // Display current info
            System.out.println("\nCURRENT INFORMATION:");
            System.out.println("   First Name: " + rs.getString("first_name"));
            System.out.println("   Last Name: " + rs.getString("last_name"));
            System.out.println("   Email: " + rs.getString("email"));

            rs.close();
            selectStmt.close();

            // Ask what to update
            System.out.println("\n🔧 What would you like to update?");
            System.out.println("1. First Name");
            System.out.println("2. Last Name");
            System.out.println("3. Email");
            System.out.println("4. Cancel");
            System.out.print("\nEnter choice (1-4): ");

            int choice = scanner.nextInt();
            // Consume newline
            scanner.nextLine();

            if (choice == 4) {
                System.out.println("Update cancelled.");
                return;
            }

            String updateQuery = "";
            String newValue = "";
            String fieldName = "";

            switch (choice) {
                case 1:
                    fieldName = "first_name";
                    System.out.print("Enter new first name: ");
                    newValue = scanner.nextLine().trim();
                    break;
                case 2:
                    fieldName = "last_name";
                    System.out.print("Enter new last name: ");
                    newValue = scanner.nextLine().trim();
                    break;
                case 3:
                    fieldName = "email";
                    System.out.print("Enter new email: ");
                    newValue = scanner.nextLine().trim();
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }

            if (newValue.isEmpty()) {
                System.out.println("ERROR: Value cannot be empty.");
                return;
            }

            // Perform update
            updateQuery = "UPDATE Member SET " + fieldName + " = ? WHERE member_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setString(1, newValue);
            updateStmt.setInt(2, memberId);

            int rowsUpdated = updateStmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("SUCCESS! Profile updated successfully.");
            } else {
                System.out.println("ERROR: Update failed.");
            }

            updateStmt.close();

        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                System.out.println("ERROR: This email is already in use by another member.");
            } else {
                System.out.println("ERROR: Update failed.");
                System.out.println("Details: " + e.getMessage());
            }
        }
    }

    // Operation: Create New Fitness Goal
    // Allows member to set a new fitness goal
    // Edge Case -> Invalid target value or date
    public static void createFitnessGoal(Connection conn, int memberId, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("        CREATE NEW FITNESS GOAL");
        System.out.println("========================================");

        try {
            // Goal type
            System.out.println("\nWhat type of goal would you like to set?");
            System.out.println("1. Weight Loss");
            System.out.println("2. Muscle Gain");
            System.out.println("3. Body Fat Reduction");
            System.out.println("4. VO2 Max Improvement");
            System.out.println("5. Other");
            System.out.print("\nEnter choice (1-5): ");

            int typeChoice = scanner.nextInt();
            scanner.nextLine();

            String goalType = "";
            switch (typeChoice) {
                case 1: goalType = "Weight Loss"; break;
                case 2: goalType = "Muscle Gain"; break;
                case 3: goalType = "Body Fat Reduction"; break;
                case 4: goalType = "VO2 Max Improvement"; break;
                case 5:
                    System.out.print("Enter custom goal type: ");
                    goalType = scanner.nextLine().trim();
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }

            // Target value
            System.out.print("\nEnter target value (e.g., 75 for 75kg, 20 for 20% body fat): ");
            double targetValue = scanner.nextDouble();
            scanner.nextLine();

            if (targetValue <= 0) {
                System.out.println("ERROR: Target value must be positive.");
                return;
            }

            // Target date
            System.out.print("Enter target date (YYYY-MM-DD): ");
            String dateString = scanner.nextLine().trim();
            Date targetDate = null;

            try {
                targetDate = Date.valueOf(dateString);

                // Check if date is in the future
                if (targetDate.before(new Date(System.currentTimeMillis()))) {
                    System.out.println("ERROR: Target date must be in the future.");
                    return;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Invalid date format. Use YYYY-MM-DD");
                return;
            }

            // Insert goal
            String query = "INSERT INTO FitnessGoal (member_id, goal_type, target_value, target_date, status) " +
                    "VALUES (?, ?, ?, ?, 'Active')";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, memberId);
            pstmt.setString(2, goalType);
            pstmt.setDouble(3, targetValue);
            pstmt.setDate(4, targetDate);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("\nSUCCESS! Fitness goal created.");
                System.out.println("   Goal Type: " + goalType);
                System.out.println("   Target Value: " + targetValue);
                System.out.println("   Target Date: " + targetDate);
                System.out.println("   Status: Active");
            }

            pstmt.close();

        } catch (SQLException e) {
            System.out.println("ERROR: Failed to create fitness goal.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    // Operation: Log New Health Metric
    // Allows member to record new health measurements (append, not overwrite)
    // Edge Case -> Invalid metric values (negative numbers, etc.)
    public static void logHealthMetric(Connection conn, int memberId, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("         LOG NEW HEALTH METRIC");
        System.out.println("========================================");

        System.out.println("\nEnter your current health measurements:");
        System.out.println("(Leave blank to skip any metric)\n");

        try {
            // Weight
            System.out.print("Weight (kg): ");
            String weightStr = scanner.nextLine().trim();
            Double weight = weightStr.isEmpty() ? null : Double.parseDouble(weightStr);

            // Resting Heart Rate
            System.out.print("Resting Heart Rate (bpm): ");
            String hrStr = scanner.nextLine().trim();
            Integer restingHeartRate = hrStr.isEmpty() ? null : Integer.parseInt(hrStr);

            // Body Fat Percentage
            System.out.print("Body Fat Percentage (%): ");
            String bfStr = scanner.nextLine().trim();
            Double bodyFatPercentage = bfStr.isEmpty() ? null : Double.parseDouble(bfStr);

            // VO2 Max
            System.out.print("VO2 Max (ml/kg/min): ");
            String vo2Str = scanner.nextLine().trim();
            Double vo2Max = vo2Str.isEmpty() ? null : Double.parseDouble(vo2Str);

            // Validate at least one metric was entered
            if (weight == null && restingHeartRate == null && bodyFatPercentage == null && vo2Max == null) {
                System.out.println("ERROR: Please enter at least one health metric.");
                return;
            }

            // Validate positive values
            if ((weight != null && weight <= 0) ||
                    (restingHeartRate != null && restingHeartRate <= 0) ||
                    (bodyFatPercentage != null && (bodyFatPercentage <= 0 || bodyFatPercentage > 100)) ||
                    (vo2Max != null && vo2Max <= 0)) {
                System.out.println("ERROR: All metrics must be positive values.");
                return;
            }

            // Insert health metric
            String query = "INSERT INTO HealthMetric (member_id, date_recorded, weight, resting_heart_rate, " +
                    "body_fat_percentage, vo2_max) " +
                    "VALUES (?, CURRENT_DATE, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, memberId);

            if (weight != null) pstmt.setDouble(2, weight);
            else pstmt.setNull(2, java.sql.Types.DECIMAL);

            if (restingHeartRate != null) pstmt.setInt(3, restingHeartRate);
            else pstmt.setNull(3, java.sql.Types.INTEGER);

            if (bodyFatPercentage != null) pstmt.setDouble(4, bodyFatPercentage);
            else pstmt.setNull(4, java.sql.Types.DECIMAL);

            if (vo2Max != null) pstmt.setDouble(5, vo2Max);
            else pstmt.setNull(5, java.sql.Types.DECIMAL);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("\nSUCCESS! Health metrics logged for today.");
                System.out.println("You can track your progress in the Dashboard!");
            }

            pstmt.close();

        } catch (NumberFormatException e) {
            System.out.println("ERROR: Invalid number format. Please enter valid numeric values.");
        } catch (SQLException e) {
            System.out.println("ERROR: Failed to log health metrics.");
            System.out.println("Details: " + e.getMessage());
        }
    }


    // Operation: Register for Group Class
    // Allows member to register for an upcoming group fitness class
    // Edge Case -> Class is full (trigger fires), Already registered (UNIQUE constraint)
    public static void registerForGroupClass(Connection conn, int memberId, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("      REGISTER FOR GROUP CLASS");
        System.out.println("========================================");

        try {
            // Display available upcoming classes
            System.out.println("\nAVAILABLE UPCOMING CLASSES:\n");

            String query = "SELECT gc.class_id, gc.class_name, gc.class_date, " +
                    "gc.start_time, gc.end_time, gc.capacity, " +
                    "COUNT(cr.registration_id) as current_count, " +
                    "r.room_name, " +
                    "t.first_name || ' ' || t.last_name as trainer_name " +
                    "FROM GroupClass gc " +
                    "JOIN Trainer t ON gc.trainer_id = t.trainer_id " +
                    "JOIN Room r ON gc.room_id = r.room_id " +
                    "LEFT JOIN ClassRegistration cr ON gc.class_id = cr.class_id " +
                    "WHERE gc.class_date >= CURRENT_DATE " +
                    "GROUP BY gc.class_id, r.room_name, t.first_name, t.last_name " +
                    "ORDER BY gc.class_date, gc.start_time";

            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            boolean hasClasses = false;
            System.out.println("ID   | Class Name              | Date       | Time        | Room       | Trainer          | Spots");
            System.out.println("-----+-------------------------+------------+-------------+------------+------------------+-------");

            while (rs.next()) {
                hasClasses = true;
                int classId = rs.getInt("class_id");
                String className = rs.getString("class_name");
                Date classDate = rs.getDate("class_date");
                Time startTime = rs.getTime("start_time");
                Time endTime = rs.getTime("end_time");
                int capacity = rs.getInt("capacity");
                int currentCount = rs.getInt("current_count");
                String roomName = rs.getString("room_name");
                String trainerName = rs.getString("trainer_name");

                int availableSpots = capacity - currentCount;
                String spotsDisplay = availableSpots > 0 ? availableSpots + "/" + capacity : "FULL";

                System.out.printf("%-4d | %-23s | %s | %s-%s | %-10s | %-16s | %s\n",
                        classId, className, classDate,
                        startTime.toString().substring(0, 5),
                        endTime.toString().substring(0, 5),
                        roomName, trainerName, spotsDisplay);
            }

            if (!hasClasses) {
                System.out.println("No upcoming classes available at this time.");
                rs.close();
                pstmt.close();
                return;
            }

            rs.close();
            pstmt.close();

            // Get user input
            System.out.print("\nEnter Class ID to register (0 to cancel): ");
            int classId = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (classId == 0) {
                System.out.println("Registration cancelled.");
                return;
            }

            // Check if class exists and is upcoming
            String validateQuery = "SELECT class_name, class_date FROM GroupClass WHERE class_id = ? AND class_date >= CURRENT_DATE";
            PreparedStatement validateStmt = conn.prepareStatement(validateQuery);
            validateStmt.setInt(1, classId);
            ResultSet validateRs = validateStmt.executeQuery();

            if (!validateRs.next()) {
                System.out.println("ERROR: Invalid class ID or class is in the past.");
                validateRs.close();
                validateStmt.close();
                return;
            }

            String className = validateRs.getString("class_name");
            Date classDate = validateRs.getDate("class_date");
            validateRs.close();
            validateStmt.close();

            // Insert registration (trigger will check capacity)
            String insertQuery = "INSERT INTO ClassRegistration (member_id, class_id) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setInt(1, memberId);
            insertStmt.setInt(2, classId);
            insertStmt.executeUpdate();
            insertStmt.close();

            System.out.println("\nSUCCESS! You are now registered for:");
            System.out.println("   Class: " + className);
            System.out.println("   Date: " + classDate);

        } catch (SQLException e) {
            // Handle specific error cases
            if (e.getMessage().contains("Class is full")) {
                System.out.println("ERROR: This class is already at full capacity.");
                System.out.println("Please choose a different class.");
            } else if (e.getMessage().contains("duplicate key") || e.getMessage().contains("already exists")) {
                System.out.println("ERROR: You are already registered for this class.");
            } else {
                System.out.println("ERROR: Registration failed.");
                System.out.println("Details: " + e.getMessage());
            }
        }
    }
}
