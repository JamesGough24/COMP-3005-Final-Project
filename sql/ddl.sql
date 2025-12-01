-- Drop tables if they exist
DROP TABLE IF EXISTS ClassRegistration CASCADE;
DROP TABLE IF EXISTS HealthMetric CASCADE;
DROP TABLE IF EXISTS FitnessGoal CASCADE;
DROP TABLE IF EXISTS GroupClass CASCADE;
DROP TABLE IF EXISTS TrainerAvailability CASCADE;
DROP TABLE IF EXISTS Room CASCADE;
DROP TABLE IF EXISTS Trainer CASCADE;
DROP TABLE IF EXISTS Member CASCADE;
DROP TABLE IF EXISTS Admin CASCADE;

-- 1. MEMBER TABLE
CREATE TABLE Member (
    member_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    registration_date DATE DEFAULT CURRENT_DATE
);

-- 2. TRAINER TABLE
CREATE TABLE Trainer (
    trainer_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

-- 3. ADMIN TABLE
CREATE TABLE Admin (
    admin_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

-- 4. ROOM TABLE
CREATE TABLE Room (
    room_id SERIAL PRIMARY KEY,
    room_name VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL CHECK (capacity > 0)
);

-- 5. TRAINER AVAILABILITY TABLE
CREATE TABLE TrainerAvailability (
    availability_id SERIAL PRIMARY KEY,
    trainer_id INT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (trainer_id) REFERENCES Trainer(trainer_id) ON DELETE CASCADE,
    CHECK (start_time < end_time)
);

-- 6. GROUP CLASS TABLE
CREATE TABLE GroupClass (
    class_id SERIAL PRIMARY KEY,
    class_name VARCHAR(100) NOT NULL,
    class_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    trainer_id INT NOT NULL,
    room_id INT NOT NULL,
    FOREIGN KEY (trainer_id) REFERENCES Trainer(trainer_id),
    FOREIGN KEY (room_id) REFERENCES Room(room_id),
    CHECK (start_time < end_time)
);

-- 7. CLASS REGISTRATION TABLE (From M:N relationship)
CREATE TABLE ClassRegistration (
    registration_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL,
    class_id INT NOT NULL,
    registration_date DATE DEFAULT CURRENT_DATE,
    FOREIGN KEY (member_id) REFERENCES Member(member_id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES GroupClass(class_id) ON DELETE CASCADE,
    UNIQUE (member_id, class_id)  -- Prevent duplicate registrations
);

-- 8. HEALTH METRIC TABLE
CREATE TABLE HealthMetric (
    metric_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL,
    date_recorded DATE NOT NULL DEFAULT CURRENT_DATE,
    weight DECIMAL(5,2),
    resting_heart_rate INT,
    body_fat_percentage DECIMAL(4,2),
    vo2_max DECIMAL(4,2),
    FOREIGN KEY (member_id) REFERENCES Member(member_id) ON DELETE CASCADE
);

-- 9. FITNESS GOAL TABLE
CREATE TABLE FitnessGoal (
    goal_id SERIAL PRIMARY KEY,
    member_id INT NOT NULL,
    goal_type VARCHAR(50) NOT NULL,
    target_value DECIMAL(5,2),
    target_date DATE,
    status VARCHAR(20) DEFAULT 'Active' CHECK (status IN ('Active', 'Achieved', 'Abandoned')),
    FOREIGN KEY (member_id) REFERENCES Member(member_id) ON DELETE CASCADE
);

-- TRIGGER 1: Prevent Overlapping Trainer Availability
CREATE OR REPLACE FUNCTION check_trainer_availability_overlap()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM TrainerAvailability
        WHERE trainer_id = NEW.trainer_id
          AND day_of_week = NEW.day_of_week
          AND availability_id != COALESCE(NEW.availability_id, -1)
          AND (
              (NEW.start_time >= start_time AND NEW.start_time < end_time) OR
              (NEW.end_time > start_time AND NEW.end_time <= end_time) OR
              (NEW.start_time <= start_time AND NEW.end_time >= end_time)
          )
    ) THEN
        RAISE EXCEPTION 'Trainer availability overlaps with existing time slot on %', NEW.day_of_week;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER prevent_trainer_availability_overlap
BEFORE INSERT OR UPDATE ON TrainerAvailability
FOR EACH ROW
EXECUTE PROCEDURE check_trainer_availability_overlap();

-- TRIGGER 2: Prevent Class Capacity > Room Capacity
CREATE OR REPLACE FUNCTION check_class_capacity()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
DECLARE
    room_cap INT;
BEGIN
    SELECT capacity INTO room_cap
    FROM Room
    WHERE room_id = NEW.room_id;
    
    IF NEW.capacity > room_cap THEN
        RAISE EXCEPTION 'Class capacity (%) exceeds room capacity (%)', NEW.capacity, room_cap;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER enforce_class_capacity_limit
BEFORE INSERT OR UPDATE ON GroupClass
FOR EACH ROW
EXECUTE PROCEDURE check_class_capacity();

-- TRIGGER 3: Prevent Class Registration if Full
CREATE OR REPLACE FUNCTION check_class_full()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
DECLARE
    class_cap INT;
    current_count INT;
BEGIN
    SELECT capacity INTO class_cap
    FROM GroupClass
    WHERE class_id = NEW.class_id;
    
    SELECT COUNT(*) INTO current_count
    FROM ClassRegistration
    WHERE class_id = NEW.class_id;
    
    IF current_count >= class_cap THEN
        RAISE EXCEPTION 'Class is full. Cannot register more members.';
    END IF;
    
    RETURN NEW;
END;
$$;

CREATE TRIGGER prevent_class_overfill
BEFORE INSERT ON ClassRegistration
FOR EACH ROW
EXECUTE PROCEDURE check_class_full();

-- TRIGGER 4: Prevent Double Booking Room
CREATE OR REPLACE FUNCTION check_room_double_booking()
RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN
    IF EXISTS (
    SELECT 1 FROM GroupClass
    WHERE room_id = NEW.room_id
      AND class_date = NEW.class_date
      AND (
        (NEW.start_time >= start_time AND NEW.start_time < end_time) OR
        (NEW.end_time > start_time AND NEW.end_time <= end_time) OR
        (NEW.start_time <= start_time AND NEW.end_time >= end_time)
      )
  ) THEN RAISE EXCEPTION 'Room is already booked at this time';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER prevent_room_double_booking
BEFORE INSERT OR UPDATE ON GroupClass
FOR EACH ROW
EXECUTE PROCEDURE check_room_double_booking();

-- VIEW: Member Dashboard
CREATE VIEW MemberDashboard AS
SELECT 
    m.member_id,
    m.first_name,
    m.last_name,
    m.email,
    
    -- Latest health metrics
    (SELECT hm.weight 
     FROM HealthMetric hm 
     WHERE hm.member_id = m.member_id 
     ORDER BY hm.date_recorded DESC 
     LIMIT 1) AS latest_weight,
    
    (SELECT hm.resting_heart_rate 
     FROM HealthMetric hm 
     WHERE hm.member_id = m.member_id 
     ORDER BY hm.date_recorded DESC 
     LIMIT 1) AS latest_heart_rate,
    
    (SELECT hm.body_fat_percentage 
     FROM HealthMetric hm 
     WHERE hm.member_id = m.member_id 
     ORDER BY hm.date_recorded DESC 
     LIMIT 1) AS latest_body_fat,
    
    (SELECT hm.date_recorded 
     FROM HealthMetric hm 
     WHERE hm.member_id = m.member_id 
     ORDER BY hm.date_recorded DESC 
     LIMIT 1) AS latest_metric_date,
    
    -- Active fitness goals
    (SELECT COUNT(*) 
     FROM FitnessGoal fg 
     WHERE fg.member_id = m.member_id 
       AND fg.status = 'Active') AS active_goals_count,
    
    -- Total past classes attended
    (SELECT COUNT(*) 
     FROM ClassRegistration cr 
     JOIN GroupClass gc ON cr.class_id = gc.class_id 
     WHERE cr.member_id = m.member_id 
       AND gc.class_date < CURRENT_DATE) AS past_classes_count,
    
    -- Upcoming classes
    (SELECT COUNT(*) 
     FROM ClassRegistration cr 
     JOIN GroupClass gc ON cr.class_id = gc.class_id 
     WHERE cr.member_id = m.member_id 
       AND gc.class_date >= CURRENT_DATE) AS upcoming_classes_count

FROM Member m;

-- INDEX: Speed up member email lookups
CREATE INDEX idx_member_email ON Member(email);
CREATE INDEX idx_class_registration_member ON ClassRegistration(member_id);
CREATE INDEX idx_class_registration_class ON ClassRegistration(class_id);
CREATE INDEX idx_health_metric_member_date ON HealthMetric(member_id, date_recorded DESC);
CREATE INDEX idx_group_class_date ON GroupClass(class_date);
