package com.nba.service;

import com.nba.dto.StaffDto;
import com.nba.exception.InvalidArgumentsException;
import com.nba.exception.InvalidStaffDataException;
import com.nba.exception.StaffNotFoundException;
import com.nba.model.*;
import com.nba.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamManager {

    private final StaffRepository staffRepository;

    public TeamManager(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    public void validateStaffDtoToUpdate(int id, StaffDto dto) {
        Staff existingStaff = getStaffById(id);

        if ("player".equalsIgnoreCase(dto.type) && !(existingStaff instanceof Player)) {
            throw new InvalidStaffDataException("type should be player because existing staff is player");
        }
        if ("coach".equalsIgnoreCase(dto.type) && !(existingStaff instanceof Coach)) {
            throw new InvalidStaffDataException("type should be coach because existing staff is coach");
        }
        if (dto.name == null || dto.name.isEmpty()) {
            throw new InvalidStaffDataException("Name should be not empty");
        }
        if (dto.baseSalary == 0) {
            throw new InvalidStaffDataException("Base salary should be not empty");
        }

        if ("player".equalsIgnoreCase(dto.type)) {
            if (dto.rating == 0) throw new InvalidStaffDataException("Rating should be not empty");
            if (dto.positions == null || dto.positions.isEmpty()) {
                throw new InvalidStaffDataException("Positions should be not empty");
            }
        }
        if ("coach".equalsIgnoreCase(dto.type)) {
            if (dto.championshipsWon == 0) throw new InvalidStaffDataException("Championship won should be not empty");
            if (dto.experienceYears == 0) throw new InvalidStaffDataException("Experience years should be not empty");
        }
    }

    public void updateStaff(int id, StaffDto dto) {
        Staff existingStaff = getStaffById(id);
        validateStaffDtoToUpdate(id, dto);

        existingStaff.setName(dto.name);
        existingStaff.setBaseSalary(dto.baseSalary);

        if (existingStaff instanceof Player existingPlayer) {
            existingPlayer.setRating(dto.rating);
            existingPlayer.setPositions(new java.util.HashSet<>(dto.positions));
        } else if (existingStaff instanceof Coach existingCoach) {
            existingCoach.setChampionshipsWon(dto.championshipsWon);
            existingCoach.setExperienceYears(dto.experienceYears);
        }

        staffRepository.save(existingStaff);
    }

    public void patchStaff(int id, StaffDto dto) {
        Staff existingStaff = getStaffById(id);

        if (dto.name != null && !dto.name.isEmpty()) {
            existingStaff.setName(dto.name);
        }
        if (dto.baseSalary != 0) {
            existingStaff.setBaseSalary(dto.baseSalary);
        }

        if (existingStaff instanceof Player) {
            if (dto.experienceYears != 0 || dto.championshipsWon != 0) {
                throw new InvalidStaffDataException("Cannot update Coach specific fields (experienceYears, championshipsWon) for a Player.");
            }

            Player player = (Player) existingStaff;
            if (dto.rating != 0) player.setRating(dto.rating);
            if (dto.positions != null && !dto.positions.isEmpty()) {
                player.setPositions(new java.util.HashSet<>(dto.positions));
            }

        } else if (existingStaff instanceof Coach) {
            if (dto.rating != 0 || (dto.positions != null && !dto.positions.isEmpty())) {
                throw new InvalidStaffDataException("Cannot update Player specific fields (rating, positions) for a Coach.");
            }

            Coach coach = (Coach) existingStaff;
            if (dto.experienceYears != 0) coach.setExperienceYears(dto.experienceYears);
            if (dto.championshipsWon != 0) coach.setChampionshipsWon(dto.championshipsWon);
        }

        staffRepository.save(existingStaff);
    }

    public Staff convertToStaff(StaffDto dto) {
        if ("player".equalsIgnoreCase(dto.type)) {
            return new Player(dto.name, dto.baseSalary, dto.rating, dto.positions.toArray(new Position[0]));
        } else if ("coach".equalsIgnoreCase(dto.type)) {
            return new Coach(dto.name, dto.baseSalary, dto.experienceYears, dto.championshipsWon);
        }
        throw new InvalidStaffDataException("Unknown staff type, type should be player or coach");
    }

    public void addStaff(Staff staff) {
        staffRepository.save(staff);
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    public Staff getStaffById(int id) {
        return staffRepository.findById(id).orElseThrow(() -> new StaffNotFoundException(id));
    }

    public void removeStaff(int id) {
        if (!staffRepository.existsById(id)) throw new StaffNotFoundException(id);
        staffRepository.deleteById(id);
    }

    public List<Player> getPlayers() {
        return staffRepository.findAll().stream()
                .filter(s -> s instanceof Player).map(s -> (Player) s).collect(Collectors.toList());
    }

    public List<Coach> getCoaches() {
        return staffRepository.findAll().stream()
                .filter(s -> s instanceof Coach).map(s -> (Coach) s).collect(Collectors.toList());
    }

    public List<Player> getPlayersByPositions(Position... positions) {
        if (positions == null || positions.length == 0) throw new InvalidArgumentsException("Positions required");
        Set<Position> searchSet = Set.of(positions);
        return getPlayers().stream()
                .filter(p -> p.getPositions().stream().anyMatch(searchSet::contains))
                .collect(Collectors.toList());
    }

    public List<Player> getPlayersByBonus(double minBonus) {
        if (minBonus < 0) throw new InvalidArgumentsException("Bonus must be positive");
        return getPlayers().stream().filter(p -> p.calculateBonus() >= minBonus).collect(Collectors.toList());
    }

    public List<Coach> getCoachesByChampionshipWon(int minChampionshipWon) {
        return getCoaches().stream().filter(c -> c.getChampionshipsWon() >= minChampionshipWon).collect(Collectors.toList());
    }

    public List<Coach> getCoachByExperienceYears(int minExperienceYears) {
        return getCoaches().stream().filter(c -> c.getExperienceYears() >= minExperienceYears).collect(Collectors.toList());
    }

    public List<Staff> getByName(String name) {
        return staffRepository.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Staff> getStaffByBaseSalary(double minBaseSalary) {
        return staffRepository.findAll().stream().filter(s -> s.getBaseSalary() >= minBaseSalary).collect(Collectors.toList());
    }

    public List<Player> getHighestRatingPlayers() {
        List<Player> players = getPlayers();
        int max = players.stream().mapToInt(Player::getRating).max().orElse(-1);
        return players.stream().filter(p -> p.getRating() == max).collect(Collectors.toList());
    }

    public List<Staff> getHighestPaidStaff() {
        List<Staff> all = getAllStaff();
        double max = all.stream().mapToDouble(Staff::calculateTotalSalary).max().orElse(-1);
        return all.stream().filter(s -> s.calculateTotalSalary() == max).collect(Collectors.toList());
    }
}