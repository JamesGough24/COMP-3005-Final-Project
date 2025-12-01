-- Clear existing data (in specific order due to foreign keys)
DELETE FROM ClassRegistration;
DELETE FROM HealthMetric;
DELETE FROM FitnessGoal;
DELETE FROM GroupClass;
DELETE FROM TrainerAvailability;
DELETE FROM Room;
DELETE FROM Trainer;
DELETE FROM Member;
DELETE FROM Admin;


-- Reset sequences
ALTER SEQUENCE member_member_id_seq RESTART WITH 1;
ALTER SEQUENCE trainer_trainer_id_seq RESTART WITH 1;
ALTER SEQUENCE admin_admin_id_seq RESTART WITH 1;
ALTER SEQUENCE room_room_id_seq RESTART WITH 1;
ALTER SEQUENCE traineravailability_availability_id_seq RESTART WITH 1;
ALTER SEQUENCE groupclass_class_id_seq RESTART WITH 1;
ALTER SEQUENCE classregistration_registration_id_seq RESTART WITH 1;
ALTER SEQUENCE healthmetric_metric_id_seq RESTART WITH 1;
ALTER SEQUENCE fitnessgoal_goal_id_seq RESTART WITH 1;


-- 1. INSERT MEMBERS (6 members)
INSERT INTO Member (first_name, last_name, email, registration_date) VALUES
('John', 'Smith', 'john.smith@email.com', '2024-01-10'),
('Sarah', 'Johnson', 'sarah.j@email.com', '2024-01-15'),
('Michael', 'Chen', 'mchen@email.com', '2024-02-01'),
('Emily', 'Williams', 'ewilliams@email.com', '2024-02-10'),
('David', 'Brown', 'dbrown@email.com', '2024-03-05'),
('Jessica', 'Martinez', 'jmartinez@email.com', '2024-03-15');


-- 2. INSERT TRAINERS (3 trainers)
INSERT INTO Trainer (first_name, last_name, email) VALUES
('Emma', 'Wilson', 'emma.wilson@fitclub.com'),
('Marcus', 'Thompson', 'marcus.t@fitclub.com'),
('Lisa', 'Patel', 'lisa.patel@fitclub.com');


-- 3. INSERT ADMINS (2 admins)
INSERT INTO Admin (first_name, last_name, email) VALUES
('Robert', 'Johnson', 'robert.j@fitclub.com'),
('Michelle', 'Davis', 'michelle.d@fitclub.com');


-- 4. INSERT ROOMS (4 rooms)
INSERT INTO Room (room_name, capacity) VALUES
('Small Studio', 3),
('Medium Studio', 5),
('Large Studio', 10),
('Main Hall', 15);


-- 5. INSERT TRAINER AVAILABILITY
-- Emma Wilson (Yoga) - Available Mon/Wed/Fri
INSERT INTO TrainerAvailability (trainer_id, day_of_week, start_time, end_time) VALUES
(1, 'Monday', '09:00', '17:00'),
(1, 'Wednesday', '09:00', '17:00'),
(1, 'Friday', '09:00', '17:00');

-- Marcus Thompson (Strength) - Available Tue/Thu
INSERT INTO TrainerAvailability (trainer_id, day_of_week, start_time, end_time) VALUES
(2, 'Tuesday', '10:00', '18:00'),
(2, 'Thursday', '10:00', '18:00');

-- Lisa Patel (Spin) - Available Mon/Wed
INSERT INTO TrainerAvailability (trainer_id, day_of_week, start_time, end_time) VALUES
(3, 'Monday', '18:00', '21:00'),
(3, 'Wednesday', '18:00', '21:00');


-- 6. INSERT GROUP CLASSES

-- PAST CLASSES (November 2025)
INSERT INTO GroupClass (class_name, class_date, start_time, end_time, capacity, trainer_id, room_id) VALUES
('Morning Yoga', '2025-11-15', '10:00', '11:00', 3, 1, 1),   -- Small Studio (capacity 3)
('Strength Basics', '2025-11-16', '14:00', '15:00', 5, 2, 2), -- Medium Studio (capacity 5)
('Evening Spin', '2025-11-18', '19:00', '20:00', 5, 3, 2);    -- Medium Studio (capacity 5)

-- UPCOMING CLASSES
INSERT INTO GroupClass (class_name, class_date, start_time, end_time, capacity, trainer_id, room_id) VALUES
-- Class ALMOST FULL (2/3 spots taken)
('Yoga Flow', '2025-12-02', '10:00', '11:00', 3, 1, 1),       -- Small Studio, will have 2 registrations

-- Class with MEDIUM availability
('Power Strength', '2025-12-03', '11:00', '12:00', 5, 2, 2),  -- Medium Studio

-- Class with LOTS of space
('Holiday Yoga', '2025-12-10', '10:00', '11:00', 10, 1, 3),   -- Large Studio

-- Class for testing ROOM CONFLICTS (same time/room as another class if Admin tries)
('Monday Morning Spin', '2025-12-09', '19:00', '20:00', 5, 3, 2), -- Medium Studio, Mon 7pm

-- Additional upcoming classes for variety
('Beginner Strength', '2025-12-04', '14:00', '15:00', 3, 2, 1),
('Sunset Yoga', '2025-12-06', '10:00', '11:00', 10, 1, 3),
('Midweek Spin', '2025-12-11', '19:00', '20:00', 5, 3, 2);


-- 7. INSERT CLASS REGISTRATIONS

-- PAST CLASSES
INSERT INTO ClassRegistration (member_id, class_id, registration_date) VALUES
-- Morning Yoga (class_id 1) - all 3 spots filled
(1, 1, '2025-11-10'),
(2, 1, '2025-11-10'),
(3, 1, '2025-11-10'),

-- Strength Basics (class_id 2) - 3 out of 5
(1, 2, '2025-11-10'),
(4, 2, '2025-11-10'),
(5, 2, '2025-11-10'),

-- Evening Spin (class_id 3) - 2 out of 5
(2, 3, '2025-11-15'),
(3, 3, '2025-11-15');

-- UPCOMING CLASSES
-- Yoga Flow (class_id 4) - 2 out of 3 (ALMOST FULL)
INSERT INTO ClassRegistration (member_id, class_id, registration_date) VALUES
(1, 4, '2025-11-25'),
(2, 4, '2025-11-25');

-- Power Strength (class_id 5) - 2 out of 5
INSERT INTO ClassRegistration (member_id, class_id, registration_date) VALUES
(3, 5, '2025-11-25'),
(4, 5, '2025-11-25');

-- Holiday Yoga (class_id 6) - 3 out of 10 (LOTS of space)
INSERT INTO ClassRegistration (member_id, class_id, registration_date) VALUES
(1, 6, '2025-11-28'),
(2, 6, '2025-11-28'),
(5, 6, '2025-11-28');

-- Monday Morning Spin (class_id 7) - 1 out of 5
INSERT INTO ClassRegistration (member_id, class_id, registration_date) VALUES
(3, 7, '2025-11-28');

-- Beginner Strength (class_id 8) - 1 out of 3
INSERT INTO ClassRegistration (member_id, class_id, registration_date) VALUES
(4, 8, '2025-11-28');

-- Sunset Yoga (class_id 9) - 2 out of 10
INSERT INTO ClassRegistration (member_id, class_id, registration_date) VALUES
(1, 9, '2025-11-28'),
(6, 9, '2025-11-28');


-- 8. INSERT HEALTH METRICS (2-3 entries per member)
-- John Smith - Weight loss progress
INSERT INTO HealthMetric (member_id, date_recorded, weight, resting_heart_rate, body_fat_percentage, vo2_max) VALUES
(1, '2024-06-10', 90.0, 72, 24.0, 38.5),
(1, '2024-09-10', 85.0, 68, 22.0, 42.0),
(1, '2024-11-20', 82.0, 65, 20.0, 44.5);

-- Sarah Johnson - Maintenance
INSERT INTO HealthMetric (member_id, date_recorded, weight, resting_heart_rate, body_fat_percentage, vo2_max) VALUES
(2, '2024-05-15', 62.0, 58, 22.0, 46.0),
(2, '2024-11-15', 61.0, 56, 21.0, 48.0);

-- Michael Chen - Muscle gain
INSERT INTO HealthMetric (member_id, date_recorded, weight, resting_heart_rate, body_fat_percentage, vo2_max) VALUES
(3, '2024-05-01', 75.0, 65, 18.0, 43.0),
(3, '2024-11-01', 82.0, 62, 16.0, 47.0);

-- Emily Williams - Cardio improvement
INSERT INTO HealthMetric (member_id, date_recorded, weight, resting_heart_rate, body_fat_percentage, vo2_max) VALUES
(4, '2024-05-10', 68.0, 72, 26.0, 36.0),
(4, '2024-11-10', 65.0, 62, 23.0, 42.0);

-- David Brown - Getting started
INSERT INTO HealthMetric (member_id, date_recorded, weight, resting_heart_rate, body_fat_percentage, vo2_max) VALUES
(5, '2024-06-05', 95.0, 78, 28.0, 33.0),
(5, '2024-11-18', 88.0, 70, 25.0, 38.0);

-- Jessica Martinez - Consistent
INSERT INTO HealthMetric (member_id, date_recorded, weight, resting_heart_rate, body_fat_percentage, vo2_max) VALUES
(6, '2024-06-15', 58.0, 62, 23.0, 42.0),
(6, '2024-11-15', 57.0, 59, 22.0, 45.0);


-- 9. INSERT FITNESS GOALS (1 per member)
INSERT INTO FitnessGoal (member_id, goal_type, target_value, target_date, status) VALUES
(1, 'Weight Loss', 80.0, '2025-03-01', 'Active'),
(2, 'Weight Maintenance', 61.0, '2025-12-31', 'Active'),
(3, 'Muscle Gain', 85.0, '2025-02-01', 'Active'),
(4, 'Body Fat Reduction', 20.0, '2025-01-15', 'Active'),
(5, 'Weight Loss', 82.0, '2025-04-01', 'Active'),
(6, 'VO2 Max Improvement', 48.0, '2025-06-01', 'Active');
