package com.nba.controller;

import com.nba.dto.StaffDto;
import com.nba.model.Coach;
import com.nba.model.Player;
import com.nba.model.Position;
import com.nba.model.Staff;
import com.nba.service.TeamManager;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TeamManagerController {
    private final TeamManager teamManager;
    private final Path path = Path.of("nba_team.dat");

    public TeamManagerController(TeamManager teamManager) {
        this.teamManager = teamManager;
        this.teamManager.loadTeamFromFile(path);
    }

    @PostMapping("staff/add")
    public String addStaff(@RequestBody StaffDto dto) {
        Staff staff = teamManager.convertToStaff(dto);
        teamManager.addStaff(staff);
        teamManager.saveTeamToFile(path);
        return "Staff " + staff.getName() + " added successfully!";
    }

    @PutMapping("staff/{id}")
    public String updateStaff(@PathVariable int id, @RequestBody StaffDto dto) {
        teamManager.updateStaff(id, dto);
        teamManager.saveTeamToFile(path);
        return "Staff with ID " + id + " updated successfully!";
    }

    @PatchMapping("staff/{id}")
    public String patchStaff(@PathVariable int id, @RequestBody StaffDto dto) {
        teamManager.patchStaff(id, dto);
        teamManager.saveTeamToFile(path);
        return "Staff with ID " + id + " updated successfully!";
    }

    @GetMapping("staff/{id}")
    public Staff getStaffById(@PathVariable int id) {
        return teamManager.getStaffById(id);
    }

    @DeleteMapping("staff/{id}")
    public String deleteStaffById(@PathVariable int id) {
        teamManager.removeStaff(id);
        teamManager.saveTeamToFile(path);
        return "Staff with ID " + id + " was successfully removed.";
    }

    @GetMapping("/staff")
    public List<Staff> getAllStaff() {
        return teamManager.getAllStaff();
    }

    @GetMapping("/players")
    public List<Player> getAllPlayers() {
        return teamManager.getPlayers();
    }

    @GetMapping("coaches")
    public List<Coach> getAllCoaches() {
        return teamManager.getCoaches();
    }

    @GetMapping("/highest-paid")
    public List<Staff> getHighestPaid() {
        return teamManager.getHighestPaidStaff();
    }

    @GetMapping("/highest-rating")
    public List<Player> getHighestRatingPlayers() {
        return teamManager.getHighestRatingPlayers();
    }

    @GetMapping("players/bonus")
    public List<Player> getPlayersByBonus(@RequestParam double minBonus) {
        return teamManager.getPlayersByBonus(minBonus);
    }

    @GetMapping("staff/base-salary")
    public List<Staff> getStaffByBaseSalary(@RequestParam double minBaseSalary) {
        return teamManager.getStaffByBaseSalary(minBaseSalary);
    }

    @GetMapping("staff/name")
    public List<Staff> getByName(@RequestParam String name) {
        return teamManager.getByName(name);
    }

    @GetMapping("coaches/experience-year")
    public List<Coach> getCoachesByExperienceYear(@RequestParam int minExperienceYear) {
        return teamManager.getCoachByExperienceYears(minExperienceYear);
    }

    @GetMapping("coaches/championship-won")
    public List<Coach> getCoachesByChampionshipWon(@RequestParam int minChampionshipWon) {
        return teamManager.getCoachesByChampionshipWon(minChampionshipWon);
    }

    @GetMapping("players/positions")
    public List<Player> getPlayersByPosition(@RequestParam Position... position) {
        return teamManager.getPlayersByPositions(position);
    }

}