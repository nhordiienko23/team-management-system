package com.nba.service;

import com.nba.dto.StaffDto;
import com.nba.exception.InvalidArgumentsException;
import com.nba.exception.InvalidStaffDataException;
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

    public Staff convertToStaff(StaffDto dto) {
        if ("player".equalsIgnoreCase(dto.type)) {
            return new Player(dto.name, dto.baseSalary, dto.rating, dto.positions.toArray(new Position[0]));
        } else if ("coach".equalsIgnoreCase(dto.type)) {
            return new Coach(dto.name, dto.baseSalary, dto.experienceYears, dto.championshipsWon);
        }
        throw new InvalidStaffDataException("Unknown staff type");
    }

    public void validateStaffDtoToUpdate(int id,StaffDto dto){
        Staff existingStaff = team.get(id);
        if ("player".equalsIgnoreCase(dto.type)&& !(existingStaff instanceof Player)){
            throw new InvalidStaffDataException("type should be player because existing staff is player");
        }
        if ("coach".equalsIgnoreCase(dto.type)&& !(existingStaff instanceof Coach)){
            throw new InvalidStaffDataException("type should be coach because existing staff is coach");
        }
        if (dto.name== null || dto.name.isEmpty()){
            throw new InvalidStaffDataException("Name should be not empty");
        }
        if (dto.baseSalary == 0){
            throw new InvalidStaffDataException("Base salary should be not empty");
        }
        if("player".equalsIgnoreCase(dto.type)){
            if (dto.rating == 0){
                throw new InvalidStaffDataException("Rating should be not empty");
            }
            if (dto.positions == null || dto.positions.isEmpty()){
                throw new InvalidStaffDataException("Positions should be not empty and not equal 0");
            }
        }
        if ("coach".equalsIgnoreCase(dto.type)){
            if (dto.championshipsWon == 0){
                throw new InvalidStaffDataException("Championship won should be not empty");
            }
            if (dto.experienceYears == 0){
                throw new InvalidStaffDataException("Experience years should be not empty");
            }
        }
    }

    public void updateStaff(int id, StaffDto dto) {
        validateStaffExists(id);
        Staff existingStaff = team.get(id);
        validateStaffDtoToUpdate(id,dto);
        existingStaff.setName(dto.name);
        existingStaff.setBaseSalary(dto.baseSalary);
        if ("player".equalsIgnoreCase(dto.type)&& existingStaff instanceof Player){
            Player existingPlayer = (Player) existingStaff;
            existingPlayer.setRating(dto.rating);
            existingPlayer.setPositions(dto.positions.toArray(new Position[0]));
        } else if ("coach".equalsIgnoreCase(dto.type)&& existingStaff instanceof Coach){
            Coach existingCoach = (Coach) existingStaff;
            existingCoach.setChampionshipsWon(dto.championshipsWon);
            existingCoach.setExperienceYears(dto.experienceYears);
        } else {
            throw new InvalidStaffDataException("Type of Staff doesn't match to exist type");
        }
    }

    public void patchStaff(int id, StaffDto dto) {
        validateStaffExists(id);
        Staff existingStaff = team.get(id);

        // 1. Общие поля
        if (dto.name != null && !dto.name.isEmpty() && !dto.name.equals(existingStaff.getName())) {
            existingStaff.setName(dto.name);
        }
        if (dto.baseSalary != 0 && dto.baseSalary != existingStaff.getBaseSalary()) {
            existingStaff.setBaseSalary(dto.baseSalary);
        }

        // 2. Обновление через проверку типа объекта в памяти (instanceof)
        if (existingStaff instanceof Player player) {
            if (dto.rating != 0 && dto.rating != player.getRating()) {
                player.setRating(dto.rating);
            }
            if (dto.positions != null && !dto.positions.isEmpty()) {
                player.setPositions(dto.positions.toArray(new Position[0]));
            }
        } else if (existingStaff instanceof Coach coach) {
            if (dto.experienceYears != 0 && dto.experienceYears != coach.getExperienceYears()) {
                coach.setExperienceYears(dto.experienceYears);
            }
            if (dto.championshipsWon != 0 && dto.championshipsWon != coach.getChampionshipsWon()) {
                coach.setChampionshipsWon(dto.championshipsWon);
            }
        }
    }

    public void addStaff(Staff staff) {
        if (staff.getId() == 0) {
            throw new InvalidStaffDataException("Ошибка: ID объекта не был присвоен при создании");
        }
        if (team.containsKey(staff.getId())) {
            throw new InvalidStaffDataException("Staff with ID" + staff.getId() + " уже существует");
        }
        team.put(staff.getId(), staff);

    }

    private void validateStaffExists(int id) {
        if (!team.containsKey(id)) {
            throw new StaffNotFoundException(id);
        }
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff instanceof Player player) {
                players.add(player);
            }
        }
        return players;
    }

    public List<Coach> getCoaches() {
        List<Coach> coaches = new ArrayList<>();
        for (Staff staff : team.values()) {
            if (staff instanceof Coach coach) {
                coaches.add(coach);
            }
        }
        return coaches;
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
                for (Position position : player.getPositions()) {
                    if (searchSet.contains(position)) {
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
            int maxId = team.keySet().stream()
                    .max(Integer::compare)
                    .orElse(0);
            Staff.setNextId(maxId + 1);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading team: " + e.getMessage());
        }
    }

}
