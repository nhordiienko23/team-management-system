package com.nba.service;

import com.nba.exception.InvalidArgumentsException;
import com.nba.exception.StaffNotFoundException;
import com.nba.model.Coach;
import com.nba.model.Player;
import com.nba.model.Position;
import com.nba.model.Staff;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

@Service
public class TeamManager {
    private Map<Integer, Staff> team;

    public TeamManager() {
        this.team = new HashMap<>();
    }

    public void addStaff(Staff staff) {
        team.put(staff.getId(), staff);
    }

    private void validateStaffExists(int id) {
        if (!team.containsKey(id)) {
            throw new StaffNotFoundException(id);
        }
    }
    public List<Player> getPlayers(){
        List<Player> players = new ArrayList<>();
        for (Staff staff:team.values()){
            if (staff instanceof Player player){
                players.add(player);
            }
        }
        return players;
    }
    public Staff getStaffById(int id) {
        validateStaffExists(id);
        return team.get(id);
    }

    public boolean removeStaff(int id) {
        validateStaffExists(id);
        team.remove(id);
        return true;
    }

    public List<Staff> getAllStaff() {
        return new ArrayList<>(team.values());
    }

    public List<Player> getPlayersByPositions(Position... positions) {
        if (positions == null || positions.length == 0) {
            throw new InvalidArgumentsException("Position list should be not null and at least one position");
        }
        Set<Position> searchSet = Set.of(positions);
        List<Player> result = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff instanceof Player) {
                Player player = (Player) staff;
                for (Position position :player.getPositions()){
                    if (searchSet.contains(position)){
                        result.add(player);
                        break;
                    }
                }
            }
        }
        return result;
    }

    public List<Player> getPlayersByBonus(double minBonus) {
        if (minBonus < 0) {
            throw new InvalidArgumentsException("The minBonus should be positive or 0");
        }
        List<Player> players = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff instanceof Player) {
                Player currentPlayer = (Player) staff;
                if (currentPlayer.calculateBonus() >= minBonus) {
                    players.add(currentPlayer);
                }
            }
        }
        return players;
    }

    public List<Coach> getCoachesByChampionshipWon(int minChampionshipWon) {
        if (minChampionshipWon < 0) {
            throw new InvalidArgumentsException("The championship won should be positive or 0");
        }
        List<Coach> coaches = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff instanceof Coach) {
                Coach coach = (Coach) staff;
                if (coach.getChampionshipsWon() >= minChampionshipWon) {
                    coaches.add(coach);
                }
            }
        }
        return coaches;
    }

    public List<Coach> getCoachByExperienceYears(int minExperienceYears) {
        if (minExperienceYears < 0) {
            throw new InvalidArgumentsException("The experience years should be positive or 0");
        }
        List<Coach> coaches = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff instanceof Coach) {
                Coach coachCurrent = (Coach) staff;
                if (coachCurrent.getExperienceYears() >= minExperienceYears) {
                    coaches.add(coachCurrent);
                }
            }
        }
        return coaches;
    }

    public List<Staff> getByName(String name) {
        if (name == null || name.isEmpty()) {
            throw new InvalidArgumentsException("name can't be null or empty");
        }
        String lowercaseName = name.toLowerCase();
        List<Staff> result = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff.getName().toLowerCase().contains(lowercaseName)) {
                result.add(staff);
            }
        }
        return result;
    }

    public List<Staff> getStaffByBaseSalary(double minBaseSalary) {
        if (minBaseSalary <= 0) {
            throw new InvalidArgumentsException("Base salary must be bigger than 0");
        }
        List<Staff> result = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff.getBaseSalary() >= minBaseSalary) {
                result.add(staff);
            }
        }
        return result;
    }

    public List<Player> getHighestRatingPlayers() {
        List<Player> topRanked = new ArrayList<>();
        int highestRank = -1;
        for (Staff staffCurrent : team.values()) {
            if (staffCurrent instanceof Player) {
                Player player = (Player) staffCurrent;
                int rating = player.getRating();
                if (rating > highestRank) {
                    highestRank = rating;
                    topRanked.clear();
                    topRanked.add(player);
                } else if (rating == highestRank) {
                    topRanked.add(player);
                }
            }
        }
        return topRanked;
    }

    public List<Staff> getHighestPaidStaff() {
        List<Staff> topEarned = new ArrayList<>();
        double highestSalary = -1;
        for (Staff staffCurrent : team.values()) {
            double currentTotal = staffCurrent.calculateTotalSalary();
            if (currentTotal > highestSalary) {

                highestSalary = currentTotal;
                topEarned.clear();
                topEarned.add(staffCurrent);
            } else if (currentTotal == highestSalary) {
                topEarned.add(staffCurrent);
            }
        }
        return topEarned;
    }

    public void saveTeamToFile(Path path) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            objectOutputStream.writeObject(team);
            System.out.println("Team data saved successfully to " + path.getFileName());
        } catch (IOException e) {
            System.err.println("Error saving team: " + e.getMessage());
        }
    }

    public void loadTeamFromFile(Path path) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            team = (Map<Integer, Staff>) objectInputStream.readObject();
            System.out.println("Team data loaded successfully from " + path.getFileName());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading team: " + e.getMessage());
        }
    }

}
