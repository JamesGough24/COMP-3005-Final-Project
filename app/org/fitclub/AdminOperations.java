package org.fitclub;

import java.sql.*;
import java.util.Scanner;

// Handles all Admin-related database operations
public class AdminOperations {

    // Operation: Create Group Class (Book Room for Class)
    // Admin creates a new group class with room, trainer, date/time assignment
    // Validates room availability, trainer availability, capacity limits
    // Edge Case -> Room double-booked (trigger fires), Class capacity exceeds room capacity (trigger fires),
    //              Trainer not available at that time (manual validation)
    public static void createGroupClass(Connection conn, Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("        CREATE NEW GROUP CLASS");
        System.out.println("========================================");

        try {
            // Step 1: Class Name
            System.out.print("\nEnter class name (e.g., 'Morning Yoga'): ");
            String className = scanner.nextLine().trim();

            if (className.isEmpty()) {
                System.out.println("ERROR: Class name cannot be empty.");
                return;
            }

            // Step 2: Date
            System.out.print("Enter class date (YYYY-MM-DD): ");
            String dateString = scanner.nextLine().trim();
            Date classDate;

            try {
                classDate = Date.valueOf(dateString);

                // Ensure date is not in the past
                if (classDate.before(new Date(System.currentTimeMillis()))) {
                    System.out.println("ERROR: Cannot create class in the past.");
                    return;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Invalid date format. Use YYYY-MM-DD");
                return;
            }

            // Step 3: Time
            System.out.print("Enter start time (HH:MM in 24-hour format): ");
            String startTimeStr = scanner.nextLine().trim();
            Time startTime;

            try {
                startTime = Time.valueOf(startTimeStr + ":00");
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Invalid time format. Use HH:MM");
                return;
            }

            System.out.print("Enter end time (HH:MM in 24-hour format): ");
            String endTimeStr = scanner.nextLine().trim();
            Time endTime;

            try {
                endTime = Time.valueOf(endTimeStr + ":00");
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: Invalid time format. Use HH:MM");
                return;
            }

            if (!startTime.before(endTime)) {
                System.out.println("ERROR: End time must be after start time.");
                return;
            }

            // Step 4: Select Room
            System.out.println("\nAVAILABLE ROOMS:");
            displayRooms(conn);

            System.out.print("\nEnter Room ID: ");
            int roomId = scanner.nextInt();
            scanner.nextLine();

            // Get room capacity
            int roomCapacity = getRoomCapacity(conn, roomId);
            if (roomCapacity == -1) {
                System.out.println("ERROR: Invalid room ID.");
                return;
            }

            // Check if room is available at this time
            if (isRoomBooked(conn, roomId, classDate, startTime, endTime)) {
                System.out.println("ERROR: Room is already booked at this time.");
                System.out.println("Please choose a different room or time.");
                return;
            }

            // Step 5: Class Capacity
            System.out.print("\nEnter class capacity (max " + roomCapacity + "): ");
            int capacity = scanner.nextInt();
            scanner.nextLine();

            if (capacity <= 0) {
                System.out.println("ERROR: Capacity must be positive.");
                return;
            }

            if (capacity > roomCapacity) {
                System.out.println("ERROR: Class capacity (" + capacity + ") exceeds room capacity (" + roomCapacity + ").");
                return;
            }

            // Step 6: Select Trainer
            System.out.println("\nAVAILABLE TRAINERS:");
            displayTrainers(conn);

            System.out.print("\nEnter Trainer ID: ");
            int trainerId = scanner.nextInt();
            scanner.nextLine();

            // Validate trainer exists
            String trainerName = getTrainerName(conn, trainerId);
            if (trainerName == null) {
                System.out.println("ERROR: Invalid trainer ID.");
                return;
            }

            // Check if trainer is available at this time
            String dayOfWeek = getDayOfWeek(classDate);
            if (!isTrainerAvailable(conn, trainerId, dayOfWeek, startTime, endTime)) {
                System.out.println("ERROR: Trainer is not available at this time.");
                System.out.println("Please choose a different trainer or check their availability.");
                return;
            }

            // Check if trainer is already teaching another class at this time
            if (isTrainerTeaching(conn, trainerId, classDate, startTime, endTime)) {
                System.out.println("ERROR: Trainer is already teaching another class at this time.");
                return;
            }

            // Step 7: Create the class (triggers will validate room booking and capacity)
            String query = "INSERT INTO GroupClass (class_name, class_date, start_time, end_time, capacity, trainer_id, room_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, className);
            pstmt.setDate(2, classDate);
            pstmt.setTime(3, startTime);
            pstmt.setTime(4, endTime);
            pstmt.setInt(5, capacity);
            pstmt.setInt(6, trainerId);
            pstmt.setInt(7, roomId);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                int classId = -1;
                if (rs.next()) {
                    classId = rs.getInt(1);
                }
                rs.close();

                System.out.println("\nSUCCESS! Group class created.");
                System.out.println("   Class ID: " + classId);
                System.out.println("   Class Name: " + className);
                System.out.println("   Date: " + classDate + " (" + dayOfWeek + ")");
                System.out.println("   Time: " + startTimeStr + " - " + endTimeStr);
                System.out.println("   Room: Room ID " + roomId + " (Capacity: " + roomCapacity + ")");
                System.out.println("   Trainer: " + trainerName);
                System.out.println("   Class Capacity: " + capacity);
            }

            pstmt.close();

        } catch (SQLException e) {
            if (e.getMessage().contains("Room is already booked")) {
                System.out.println("ERROR: Room is already booked at this time (trigger blocked).");
            } else if (e.getMessage().contains("Class capacity") && e.getMessage().contains("exceeds room capacity")) {
                System.out.println("ERROR: Class capacity exceeds room capacity (trigger blocked).");
            } else {
                System.out.println("ERROR: Failed to create class.");
                System.out.println("Details: " + e.getMessage());
            }
        }
    }

    // Helper: Display all available rooms
    private static void displayRooms(Connection conn) {
        try {
            String query = "SELECT room_id, room_name, capacity FROM Room ORDER BY room_id";
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nID | Room Name     | Capacity");
            System.out.println("---+---------------+---------");

            while (rs.next()) {
                int roomId = rs.getInt("room_id");
                String roomName = rs.getString("room_name");
                int capacity = rs.getInt("capacity");

                System.out.printf("%-2d | %-13s | %d\n", roomId, roomName, capacity);
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.out.println("Could not retrieve rooms.");
        }
    }

    // Helper: Display all trainers
    private static void displayTrainers(Connection conn) {
        try {
            String query = "SELECT trainer_id, first_name, last_name, specialization FROM Trainer ORDER BY trainer_id";
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nID | Name                | Specialization");
            System.out.println("---+---------------------+--------------------");

            while (rs.next()) {
                int trainerId = rs.getInt("trainer_id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                String specialization = rs.getString("specialization");

                System.out.printf("%-2d | %-19s | %s\n", trainerId, name, specialization);
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.out.println("Could not retrieve trainers.");
        }
    }

    // Helper: Get room capacity
    private static int getRoomCapacity(Connection conn, int roomId) {
        try {
            String query = "SELECT capacity FROM Room WHERE room_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, roomId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int capacity = rs.getInt("capacity");
                rs.close();
                pstmt.close();
                return capacity;
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            // Ignore
        }

        return -1;
    }

    // Helper: Get trainer name
    private static String getTrainerName(Connection conn, int trainerId) {
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

        return null;
    }

    // Helper: Check if room is already booked at given time
    private static boolean isRoomBooked(Connection conn, int roomId, Date classDate, Time startTime, Time endTime) {
        try {
            String query = "SELECT 1 FROM GroupClass " +
                    "WHERE room_id = ? AND class_date = ? " +
                    "AND ((? >= start_time AND ? < end_time) OR " +
                    "     (? > start_time AND ? <= end_time) OR " +
                    "     (? <= start_time AND ? >= end_time))";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, roomId);
            pstmt.setDate(2, classDate);
            pstmt.setTime(3, startTime);
            pstmt.setTime(4, startTime);
            pstmt.setTime(5, endTime);
            pstmt.setTime(6, endTime);
            pstmt.setTime(7, startTime);
            pstmt.setTime(8, endTime);

            ResultSet rs = pstmt.executeQuery();
            boolean isBooked = rs.next();

            rs.close();
            pstmt.close();

            return isBooked;

        } catch (SQLException e) {
            return false;
        }
    }

    // Helper: Check if trainer is available at given day/time
    private static boolean isTrainerAvailable(Connection conn, int trainerId, String dayOfWeek, Time startTime, Time endTime) {
        try {
            String query = "SELECT 1 FROM TrainerAvailability " +
                    "WHERE trainer_id = ? AND day_of_week = ? " +
                    "AND ? >= start_time AND ? <= end_time";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, trainerId);
            pstmt.setString(2, dayOfWeek);
            pstmt.setTime(3, startTime);
            pstmt.setTime(4, endTime);

            ResultSet rs = pstmt.executeQuery();
            boolean isAvailable = rs.next();

            rs.close();
            pstmt.close();

            return isAvailable;

        } catch (SQLException e) {
            return false;
        }
    }

    // Helper: Check if trainer is already teaching another class at this time
    private static boolean isTrainerTeaching(Connection conn, int trainerId, Date classDate, Time startTime, Time endTime) {
        try {
            String query = "SELECT 1 FROM GroupClass " +
                    "WHERE trainer_id = ? AND class_date = ? " +
                    "AND ((? >= start_time AND ? < end_time) OR " +
                    "     (? > start_time AND ? <= end_time) OR " +
                    "     (? <= start_time AND ? >= end_time))";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, trainerId);
            pstmt.setDate(2, classDate);
            pstmt.setTime(3, startTime);
            pstmt.setTime(4, startTime);
            pstmt.setTime(5, endTime);
            pstmt.setTime(6, endTime);
            pstmt.setTime(7, startTime);
            pstmt.setTime(8, endTime);

            ResultSet rs = pstmt.executeQuery();
            boolean isTeaching = rs.next();

            rs.close();
            pstmt.close();

            return isTeaching;

        } catch (SQLException e) {
            return false;
        }
    }

    // Helper: Get day of week from date
    private static String getDayOfWeek(Date date) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);

        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
    }

    // Helper method: Get admin ID by email (for login)
    public static int getAdminIdByEmail(Connection conn, String email) {
        try {
            String query = "SELECT admin_id FROM Admin WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, email);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int adminId = rs.getInt("admin_id");
                rs.close();
                pstmt.close();
                return adminId;
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

    // Display admin's name (used in welcome message)
    public static String getAdminName(Connection conn, int adminId) {
        try {
            String query = "SELECT first_name, last_name FROM Admin WHERE admin_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, adminId);

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

        return "Admin";
    }
}
