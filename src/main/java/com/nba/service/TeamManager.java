package com.nba.service;

import com.nba.dto.StaffDto;
import com.nba.exception.InvalidStaffDataException;
import com.nba.exception.StaffNotFoundException;
import com.nba.model.Coach;
import com.nba.model.Player;
import com.nba.model.Position;
import com.nba.model.Staff;
import com.nba.repository.StaffRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class TeamManager {

    private final StaffRepository staffRepository;

    public TeamManager(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    private <T> Page<T> toPage(List<T> list, Pageable pageable) {
        // Safe check to handle Unpaged objects
        if (pageable.isUnpaged()) {
            return new PageImpl<>(list);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        if (start > list.size()) return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    // -- Overloaded methods for easy access in Main.java --

    public List<Staff> getAllStaff() {
        return staffRepository.findAll(Sort.by("id").ascending());
    }

    public List<Staff> getHighestPaidStaff() {
        double maxSalary = staffRepository.findAll().stream().mapToDouble(Staff::calculateTotalSalary).max().orElse(-1);
        return staffRepository.findAll().stream()
                .filter(s -> s.calculateTotalSalary() == maxSalary)
                .sorted(Comparator.comparing(Staff::getId))
                .toList();
    }

    // -- Existing methods with Pageable --

    public Page<Staff> getAllStaff(Pageable pageable) {
        return staffRepository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id").ascending()));
    }

    public void validateStaffDtoToUpdate(int id, StaffDto dto) {
        Staff existingStaff = getStaffById(id);
        if ("player".equalsIgnoreCase(dto.type) && !(existingStaff instanceof Player)) throw new InvalidStaffDataException("Type should be player");
        if ("coach".equalsIgnoreCase(dto.type) && !(existingStaff instanceof Coach)) throw new InvalidStaffDataException("Type should be coach");
        if (dto.name == null || dto.name.isEmpty()) throw new InvalidStaffDataException("Name cannot be empty");
        if (dto.baseSalary == 0) throw new InvalidStaffDataException("Base salary cannot be empty or zero");
    }

    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
    public void updateStaff(int id, StaffDto dto) {
        Staff existingStaff = getStaffById(id);
        validateStaffDtoToUpdate(id, dto);
        existingStaff.setName(dto.name);
        existingStaff.setBaseSalary(dto.baseSalary);
        if (existingStaff instanceof Player p) { p.setRating(dto.rating); p.setPositions(new HashSet<>(dto.positions)); }
        else if (existingStaff instanceof Coach c) { c.setChampionshipsWon(dto.championshipsWon); c.setExperienceYears(dto.experienceYears); }
        staffRepository.save(existingStaff);
    }

    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
    public void patchStaff(int id, StaffDto dto) {
        Staff existingStaff = getStaffById(id);
        if (dto.name != null && !dto.name.isEmpty()) existingStaff.setName(dto.name);
        if (dto.baseSalary != 0) existingStaff.setBaseSalary(dto.baseSalary);
        if (existingStaff instanceof Player p && dto.rating != 0) p.setRating(dto.rating);
        else if (existingStaff instanceof Coach c) {
            if (dto.experienceYears != 0) c.setExperienceYears(dto.experienceYears);
            if (dto.championshipsWon != 0) c.setChampionshipsWon(dto.championshipsWon);
        }
        staffRepository.save(existingStaff);
    }

    public Staff convertToStaff(StaffDto dto) {
        if ("player".equalsIgnoreCase(dto.type)) return new Player(dto.name, dto.baseSalary, dto.rating, dto.positions.toArray(new Position[0]));
        if ("coach".equalsIgnoreCase(dto.type)) return new Coach(dto.name, dto.baseSalary, dto.experienceYears, dto.championshipsWon);
        throw new InvalidStaffDataException("Unknown staff type");
    }

    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
    public void addStaff(Staff staff) { staffRepository.save(staff); }

    public Staff getStaffById(int id) { return staffRepository.findById(id).orElseThrow(() -> new StaffNotFoundException(id)); }

    @CacheEvict(value = {"highestPaidStaff", "highestRatingPlayers"}, allEntries = true)
    public void removeStaff(int id) { staffRepository.deleteById(id); }

    public Page<Player> getPlayers(Pageable p) {
        return toPage(staffRepository.findAll().stream().filter(s -> s instanceof Player).map(s -> (Player) s)
                .sorted(Comparator.comparing(Staff::getId)).toList(), p);
    }

    public Page<Coach> getCoaches(Pageable p) {
        return toPage(staffRepository.findAll().stream().filter(s -> s instanceof Coach).map(s -> (Coach) s)
                .sorted(Comparator.comparing(Staff::getId)).toList(), p);
    }

    public Page<Player> getPlayersByPositions(Position[] pos, Pageable p) {
        Set<Position> set = Set.of(pos);
        return toPage(staffRepository.findAll().stream().filter(s -> s instanceof Player).map(s -> (Player) s)
                .filter(pl -> !Collections.disjoint(pl.getPositions(), set))
                .sorted(Comparator.comparing(Staff::getId)).toList(), p);
    }

    public Page<Player> getPlayersByBonus(double min, Pageable p) {
        return toPage(staffRepository.findAll().stream().filter(s -> s instanceof Player).map(s -> (Player) s)
                .filter(pl -> pl.calculateBonus() >= min).sorted(Comparator.comparingDouble(Player::calculateBonus).reversed()).toList(), p);
    }

    public Page<Coach> getCoachesByChampionshipWon(int min, Pageable p) {
        return toPage(staffRepository.findAll().stream().filter(s -> s instanceof Coach).map(s -> (Coach) s)
                .filter(c -> c.getChampionshipsWon() >= min).sorted(Comparator.comparingInt(Coach::getChampionshipsWon).reversed()).toList(), p);
    }

    public Page<Coach> getCoachByExperienceYears(int min, Pageable p) {
        return toPage(staffRepository.findAll().stream().filter(s -> s instanceof Coach).map(s -> (Coach) s)
                .filter(c -> c.getExperienceYears() >= min).sorted(Comparator.comparingInt(Coach::getExperienceYears).reversed()).toList(), p);
    }

    public Page<Staff> getByName(String name, Pageable p) {
        return toPage(staffRepository.findAll().stream().filter(s -> s.getName().toLowerCase().contains(name.toLowerCase()))
                .sorted(Comparator.comparing(Staff::getName)).toList(), p);
    }

    public Page<Staff> getStaffByBaseSalary(double min, Pageable p) {
        return toPage(staffRepository.findAll().stream().filter(s -> s.getBaseSalary() >= min)
                .sorted(Comparator.comparingDouble(Staff::getBaseSalary).reversed()).toList(), p);
    }

    @Cacheable(value = "highestRatingPlayers")
    public Page<Player> getHighestRatingPlayers(Pageable p) {
        List<Player> pl = staffRepository.findAll().stream().filter(s -> s instanceof Player).map(s -> (Player) s).toList();
        int max = pl.stream().mapToInt(Player::getRating).max().orElse(-1);
        return toPage(pl.stream().filter(p_ -> p_.getRating() == max).sorted(Comparator.comparing(Staff::getId)).toList(), p);
    }

    @Cacheable(value = "highestPaidStaff")
    public Page<Staff> getHighestPaidStaff(Pageable p) {
        double maxSalary = staffRepository.findAll().stream().mapToDouble(Staff::calculateTotalSalary).max().orElse(-1);
        return toPage(staffRepository.findAll().stream().filter(s -> s.calculateTotalSalary() == maxSalary)
                .sorted(Comparator.comparing(Staff::getId)).toList(), p);
    }
}