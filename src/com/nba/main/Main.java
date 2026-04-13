package com.nba.main;

import com.nba.model.*;
import com.nba.service.TeamManager;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        TeamManager teamManager = getTeamManager();
        System.out.println("=== Chicago Bulls Roster ===");
        for (Staff member : teamManager.getAllStaff()) {
            System.out.println(member);
        }
        System.out.println("\n--- Financial Analysis ---");
        List<Staff> stars = teamManager.getHighestPaidStaff();
        if (!stars.isEmpty()) {
            System.out.println("Top Earners of the Team (" + stars.size() + " members found):");
            for (Staff star : stars) {
                System.out.printf(" - %-15s | Compensation: $%,.0f\n",
                        star.getName(), star.calculateTotalSalary());
            }
        }

        int idToRemove = 3;
        if (teamManager.removeStaff(idToRemove)) {
            System.out.println("\nStaff with ID " + idToRemove + " has been removed.");
        }
        int idToFined = 5;
        System.out.println("\nGet staff by id " + idToFined + "\n" + teamManager.getStaffById(idToFined));
    }

    private static TeamManager getTeamManager() {
        Player jordan = new Player("Michael Jordan", 1000000, "SG", 99);
        Player pippen = new Player("Scottie Pippen", 700000, "SF", 92);
        Player rodman = new Player("Dennis Rodman", 500000, "PF", 88);
        Player leBron = new Player("LeBron James", 900000, "PF", 98);
        Player edwards = new Player("Anthony Edwards", 1000000, "PF", 98);
        Coach coach = new Coach("Phill Jackson", 20000, 20, 11);
        TeamManager teamManager = new TeamManager();
        teamManager.addStaff(jordan);
        teamManager.addStaff(pippen);
        teamManager.addStaff(rodman);
        teamManager.addStaff(leBron);
        teamManager.addStaff(edwards);
        teamManager.addStaff(coach);
        return teamManager;
    }
}
