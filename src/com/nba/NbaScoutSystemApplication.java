package com.nba;

import com.nba.exception.InvalidStaffDataException;
import com.nba.exception.StaffNotFoundException;
import com.nba.model.*;
import com.nba.service.TeamManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.util.List;
import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class NbaScoutSystemApplication implements CommandLineRunner {

    private final TeamManager teamManager;
    public NbaScoutSystemApplication(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(NbaScoutSystemApplication.class, args);
    }

    @Override
    public void run(String... args) {
        if (teamManager.getAllStaff().isEmpty()) {
            seedDatabase();
        }

        printTeamRoster();
        printFinancialAnalysis();
        runValidationTests();
    }

    private void seedDatabase() {
        teamManager.addStaff(new Player("Michael Jordan", 1000000, 99, Position.SG, Position.SF));
        teamManager.addStaff(new Player("Scottie Pippen", 700000, 92, Position.SF, Position.PF));
        teamManager.addStaff(new Coach("Phil Jackson", 20000, 20, 11));
        teamManager.addStaff(new Coach("Steve Kerr", 100000, 15, 4));
        teamManager.addStaff(new Player("Dennis Rodman", 500000, 88, Position.PF, Position.C));
        teamManager.addStaff(new Player("LeBron James", 900000, 98, Position.SF, Position.PF, Position.PG));
        teamManager.addStaff(new Player("Anthony Edwards", 1000000, 98, Position.SG));
        teamManager.addStaff(new Player("Karl Anthony Towns", 1, 98, Position.SG));
        teamManager.addStaff(new Coach("JJ Redick", 70000,5,0));
    }

    private void printTeamRoster() {
        System.out.println("=== Current Team Roster ===");
        teamManager.getAllStaff().forEach(System.out::println);
    }

    private void printFinancialAnalysis() {
        System.out.println("\n--- Financial Analysis ---");
        List<Staff> stars = teamManager.getHighestPaidStaff();
        stars.forEach(s -> System.out.printf(" - %-15s | Compensation: $%,.0f\n", s.getName(), s.calculateTotalSalary()));
    }

    private void runValidationTests() {
        System.out.println("\n--- Testing Data Validation ---");
        try {
            teamManager.addStaff(new Player("Fake Star", 500000, 150, Position.C));
        } catch (InvalidStaffDataException e) {
            System.out.println("Validation Caught: " + e.getMessage());
        }
    }
}