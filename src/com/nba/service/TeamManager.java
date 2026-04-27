package com.nba.service;

import com.nba.dto.StaffDto;
import com.nba.exception.InvalidArgumentsException;
import com.nba.exception.InvalidStaffDataException;
import com.nba.exception.StaffNotFoundException;
import com.nba.model.*;
import com.nba.repository.StaffRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
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
            throw new InvalidStaffDataException("Type should be player because existing staff is a player");
        }
        if ("coach".equalsIgnoreCase(dto.type) && !(existingStaff instanceof Coach)) {
            throw new InvalidStaffDataException("Type should be coach because existing staff is a coach");
        }
        if (dto.name == null || dto.name.isEmpty()) {
            throw new InvalidStaffDataException("Name cannot be empty");
        }
        if (dto.baseSalary == 0) {
            throw new InvalidStaffDataException("Base salary cannot be empty or zero");
        }

        if ("player".equalsIgnoreCase(dto.type)) {
            if (dto.rating == 0) throw new InvalidStaffDataException("Rating cannot be empty or zero");
            if (dto.positions == null || dto.positions.isEmpty()) {
                throw new InvalidStaffDataException("Positions cannot be empty");
            }
        }
        if ("coach".equalsIgnoreCase(dto.type)) {
            if (dto.championshipsWon == 0) throw new InvalidStaffDataException("Championships won cannot be empty");
            if (dto.experienceYears == 0) throw new InvalidStaffDataException("Experience years cannot be empty or zero");
        }
    }

    // Clear the cache whenever staff data is updated
    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
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

    // Clear the cache whenever staff data is patched
    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
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
        throw new InvalidStaffDataException("Unknown staff type; type should be player or coach");
    }

    // Clear the cache when a new staff member is added
    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
    public void addStaff(Staff staff) {
        staffRepository.save(staff);
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAllWithPositions(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Staff getStaffById(int id) {
        return staffRepository.findById(id).orElseThrow(() -> new StaffNotFoundException(id));
    }

    // Clear the cache when a staff member is removed
    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
    public void removeStaff(int id) {
        if (!staffRepository.existsById(id)) throw new StaffNotFoundException(id);
        staffRepository.deleteById(id);
    }

    public List<Player> getPlayers() {
        return staffRepository.findAllWithPositions(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(s -> s instanceof Player).map(s -> (Player) s).collect(Collectors.toList());
    }

    public List<Coach> getCoaches() {
        return staffRepository.findAllWithPositions(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(s -> s instanceof Coach).map(s -> (Coach) s).collect(Collectors.toList());
    }

    public List<Player> getPlayersByPositions(Position... positions) {
        if (positions == null || positions.length == 0) throw new InvalidArgumentsException("Positions are required");
        Set<Position> searchSet = Set.of(positions);
        return getPlayers().stream()
                .filter(p -> p.getPositions().stream().anyMatch(searchSet::contains))
                .collect(Collectors.toList());
    }

    public List<Player> getPlayersByBonus(double minBonus) {
        if (minBonus < 0) throw new InvalidArgumentsException("Bonus must be positive");
        return getPlayers().stream()
                .filter(p -> p.calculateBonus() >= minBonus)
                .sorted((p1, p2) -> Double.compare(p2.calculateBonus(), p1.calculateBonus())) // Descending order
                .collect(Collectors.toList());
    }

    public List<Coach> getCoachesByChampionshipWon(int minChampionshipWon) {
        return getCoaches().stream()
                .filter(c -> c.getChampionshipsWon() >= minChampionshipWon)
                .sorted((c1, c2) -> Integer.compare(c2.getChampionshipsWon(), c1.getChampionshipsWon())) // Descending order
                .collect(Collectors.toList());
    }

    public List<Coach> getCoachByExperienceYears(int minExperienceYears) {
        return getCoaches().stream()
                .filter(c -> c.getExperienceYears() >= minExperienceYears)
                .sorted((c1, c2) -> Integer.compare(c2.getExperienceYears(), c1.getExperienceYears())) // Descending order
                .collect(Collectors.toList());
    }

    public List<Staff> getByName(String name) {
        return staffRepository.findAllWithPositions(Sort.by(Sort.Direction.ASC, "name")).stream() // Alphabetical order
                .filter(s -> s.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Staff> getStaffByBaseSalary(double minBaseSalary) {
        return staffRepository.findAllWithPositions(Sort.by(Sort.Direction.DESC, "baseSalary")).stream() // Descending order by base salary
                .filter(s -> s.getBaseSalary() >= minBaseSalary)
                .collect(Collectors.toList());
    }

    // Cache the result of this heavy analytical query
    @Cacheable(value = "highestRatingPlayers")
    public List<Player> getHighestRatingPlayers() {
        List<Player> players = getPlayers();
        int max = players.stream().mapToInt(Player::getRating).max().orElse(-1);
        return players.stream().filter(p -> p.getRating() == max).collect(Collectors.toList());
    }

    // Cache the result of this heavy analytical query
    @Cacheable(value = "highestPaidStaff")
    public List<Staff> getHighestPaidStaff() {
        List<Staff> all = getAllStaff();
        double max = all.stream().mapToDouble(Staff::calculateTotalSalary).max().orElse(-1);
        return all.stream().filter(s -> s.calculateTotalSalary() == max).collect(Collectors.toList());
    }
}