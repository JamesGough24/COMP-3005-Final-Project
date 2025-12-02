# FitClub JDBC Project

## 1. Project Structure
```
fitclub/
├── pom.xml # Since I used Maven
├── sql/
│   ├── DDL.sql
│   └── DML.sql
├── app/
│   └── org/
│       └── fitclub/
│           ├── Main.java
│           ├── DatabaseConnection.java
│           ├── MemberOperations.java
│           ├── TrainerOperations.java
│           └── AdminOperations.java
└── docs/
    └── ERD.pdf
```

## 2. Requirements
- Java 17 or higher  
- Maven  
- PostgreSQL server running and accessible  
- A database matching the credentials in `DatabaseConnection.java`

## 3. Setup Instructions

### Step 1: Create the database schema
Run the SQL files in order:

1. `sql/DDL.sql` - creates tables  
2. `sql/DML.sql` - inserts initial data  

Use `psql`, PgAdmin, or any SQL client

### Step 2: Configure database connection
Navigate to `app/org/fitclub/DatabaseConnection.java`

Update these fields to match your PostgreSQL setup:
```
private static final String URL = "jdbc:postgresql://localhost:5432/fitclub_db";
private static final String USER = "your_username";
private static final String PASSWORD = "your_password";
```

### Step 3: Build the project
From the fitclub directory (where pom.xml is located), run:
mvn clean compile

### Step 4: Run the program
Run using Maven:
```
mvn exec:java -Dexec.mainClass="org.fitclub.Main"
```

Or compile manually if Maven isn’t available:
```
javac app/org/fitclub/*.java
java -cp app org.fitclub.Main
```

## 4. Demo Video
The video demonstrating the whole project is an unlisted YouTube video and can be found at:
https://youtu.be/Y7Yt0YvT-3Y
