package com.nba.service;

import com.nba.dto.StaffDto;
import com.nba.exception.InvalidStaffDataException;
import com.nba.exception.StaffNotFoundException;
import com.nba.model.*;
import com.nba.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamManagerTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private TeamManager teamManager;
    // --- CRUD TESTS ---

    @Test
    void addStaff_ShouldCallRepositorySave() {
        Staff staff = new Player("Jordan", 1000, 99, Position.SG);
        teamManager.addStaff(staff);
        verify(staffRepository, times(1)).save(staff);
    }

    @Test
    void removeStaff_ShouldCallRepositoryDelete() {
        doNothing().when(staffRepository).deleteById(1);
        teamManager.removeStaff(1);
        verify(staffRepository, times(1)).deleteById(1);
    }

    @Test
    void updateStaff_ShouldUpdateExistingStaff() {
        Player existing = new Player("OldName", 1000, 80, Position.PG);
        when(staffRepository.findById(1)).thenReturn(Optional.of(existing));

        StaffDto dto = new StaffDto();
        dto.type = "player";
        dto.name = "NewName";
        dto.baseSalary = 5000;
        dto.rating = 90;
        dto.positions = List.of(Position.SG);

        teamManager.updateStaff(1, dto);

        assertEquals("NewName", existing.getName());
        assertEquals(5000, existing.getBaseSalary());
        verify(staffRepository).save(existing);
    }

    @Test
    void patchStaff_ShouldPartiallyUpdate() {
        Coach existing = new Coach("Coach", 1000, 10, 2);
        when(staffRepository.findById(1)).thenReturn(Optional.of(existing));

        StaffDto dto = new StaffDto();
        dto.name = "NewCoachName";

        teamManager.patchStaff(1, dto);

        assertEquals("NewCoachName", existing.getName());
        assertEquals(10, existing.getExperienceYears()); // Not changed
        verify(staffRepository).save(existing);
    }

    // --- VALIDATION & EXCEPTION TESTS ---

    @Test
    void validateStaffDtoToUpdate_ShouldThrowException_IfTypeMismatch() {
        Coach coach = new Coach("Coach", 1000, 10, 2);
        when(staffRepository.findById(1)).thenReturn(Optional.of(coach));

        StaffDto dto = new StaffDto();
        dto.type = "player"; // Trying to update Coach as Player

        assertThrows(InvalidStaffDataException.class, () -> teamManager.validateStaffDtoToUpdate(1, dto));
    }

    @Test
    void getStaffById_ShouldThrowException_WhenNotFound() {
        when(staffRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(StaffNotFoundException.class, () -> teamManager.getStaffById(99));
    }

    // --- FILTERING & PAGINATION TESTS ---
    @Test
    void getAllStaff_ReturnsSortedPage() {
        when(staffRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(Page.empty());
        assertNotNull(teamManager.getAllStaff(org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    void getPlayers_FiltersAndSorts() {
        Player p = new Player("Player", 1000, 80, Position.PG);
        when(staffRepository.findAll()).thenReturn(List.of(p));
        Page<Player> result = teamManager.getPlayers(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getPlayersByBonus_FiltersCorrectly() {
        Player p = new Player("Star", 1000, 95, Position.SG); // Bonus = 200
        when(staffRepository.findAll()).thenReturn(List.of(p));
        Page<Player> result = teamManager.getPlayersByBonus(100, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getCoachesByChampionshipWon_FiltersCorrectly() {
        Coach c = new Coach("Legend", 5000, 10, 5);
        when(staffRepository.findAll()).thenReturn(List.of(c));
        Page<Coach> result = teamManager.getCoachesByChampionshipWon(3, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getByName_FiltersBySubstring() {
        Player p = new Player("LeBron James", 1000, 80, Position.SF);
        when(staffRepository.findAll()).thenReturn(List.of(p));
        Page<Staff> result = teamManager.getByName("lebron", Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getHighestPaidStaff_CalculatesMaxCorrectly() {
        Player p = new Player("Rich", 1000000, 80, Position.SG);
        when(staffRepository.findAll()).thenReturn(List.of(p));
        Page<Staff> result = teamManager.getHighestPaidStaff(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getHighestRatingPlayers_FindsMaxRating() {
        Player p1 = new Player("Low", 1000, 50, Position.SG);
        Player p2 = new Player("High", 1000, 99, Position.SG);
        when(staffRepository.findAll()).thenReturn(List.of(p1, p2));
        Page<Player> result = teamManager.getHighestRatingPlayers(Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
        assertEquals(99, result.getContent().get(0).getRating());
    }

    @Test
    void getPlayersByPositions_FiltersCorrectly() {
        Player p1 = new Player("PG_Player", 1000, 80, Position.PG);
        Player p2 = new Player("SF_Player", 1000, 80, Position.SF);
        when(staffRepository.findAll()).thenReturn(List.of(p1, p2));

        Page<Player> result = teamManager.getPlayersByPositions(new Position[]{Position.PG}, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
        assertEquals("PG_Player", result.getContent().get(0).getName());
    }

    @Test
    void getCoachByExperienceYears_FiltersCorrectly() {
        Coach c1 = new Coach("Junior", 1000, 2, 0);
        Coach c2 = new Coach("Senior", 5000, 20, 5);
        when(staffRepository.findAll()).thenReturn(List.of(c1, c2));

        Page<Coach> result = teamManager.getCoachByExperienceYears(10, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
        assertEquals("Senior", result.getContent().get(0).getName());
    }

    @Test
    void getStaffByBaseSalary_FiltersCorrectly() {
        Player p = new Player("Rich", 500000, 80, Position.SG);
        when(staffRepository.findAll()).thenReturn(List.of(p));

        Page<Staff> result = teamManager.getStaffByBaseSalary(100000, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }
}