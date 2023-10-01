package com.Company;


import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EmployeeRecords {

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/demo_db";
        String user = "root";
        String password = "abcd";

        String query = "SELECT `Time`, `Time Out`, `Employee Name` FROM empdemo";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            Map<String, List<LocalDateTime[]>> employeeShifts = new HashMap<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");

            while (rs.next()) {
                String startTime = rs.getString("Time");
                String endTime = rs.getString("Time Out");
                String employeeName = rs.getString("Employee Name");

                // Skipping the records where startTime or endTime is null or empty
                if (startTime == null || startTime.trim().isEmpty() || endTime == null || endTime.trim().isEmpty())
                    continue;

                try {
                    // Parsing the startTime and endTime after trimming
                    LocalDateTime start = LocalDateTime.parse(startTime.trim(), formatter);
                    LocalDateTime end = LocalDateTime.parse(endTime.trim(), formatter);

                    // Adding the parsed LocalDateTime objects to the employeeShifts map
                    employeeShifts.computeIfAbsent(employeeName, k -> new ArrayList<>()).add(new LocalDateTime[]{start, end});
                } catch (Exception e) {
                    // Logging any exception that occurs during parsing
                    e.printStackTrace();
                    System.out.println("Error parsing times for Employee: " + employeeName + " Start Time: " + startTime + " End Time: " + endTime);
                }
            }

            processRecords(employeeShifts);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void processRecords(Map<String, List<LocalDateTime[]>> employeeShifts) {
        for (Map.Entry<String, List<LocalDateTime[]>> entry : employeeShifts.entrySet()) {
            String employeeName = entry.getKey();
            List<LocalDateTime[]> shifts = entry.getValue();

            // Sorting shifts by start time to properly compare between shifts and find consecutive days
            shifts.sort(Comparator.comparing(shift -> shift[0]));

            int consecutiveDaysCount = 1;

            for (int i = 0; i < shifts.size(); i++) {
                LocalDateTime[] shift = shifts.get(i);
                LocalDateTime start = shift[0];
                LocalDateTime end = shift[1];

                // Condition (c): More than 14 hours in a single shift
                long hoursWorked = Duration.between(start, end).toHours();
                if (hoursWorked > 14) {
                    System.out.println(employeeName + " has worked more than 14 hours in a single shift on " + start.toLocalDate());
                }

                // For conditions (a) and (b) we need to compare with the next shift, so we skip the last one
                if (i == shifts.size() - 1) continue;

                LocalDateTime[] nextShift = shifts.get(i + 1);
                LocalDateTime nextStart = nextShift[0];

                // Condition (b): Less than 10 hours but more than 1 hour between shifts
                long hoursBetweenShifts = Duration.between(end, nextStart).toHours();
                if (hoursBetweenShifts < 10 && hoursBetweenShifts > 1) {
                    System.out.println(employeeName + " has less than 10 hours but more than 1 hour between shifts on " + end.toLocalDate());
                }

                // Condition (a): 7 consecutive days
                if (nextStart.toLocalDate().isEqual(end.toLocalDate().plusDays(1))) {
                    consecutiveDaysCount++;
                    if (consecutiveDaysCount == 7) {
                        System.out.println(employeeName + " has worked for 7 consecutive days ending on " + nextStart.toLocalDate());
                    }
                } else {
                    consecutiveDaysCount = 1; // reset if the days are not consecutive
                }
            }
        }
    }
}


