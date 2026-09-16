package com.nba.coach;

import com.nba.AbstractIntegrationTest;
import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.core.exception.invalidData.InvalidCoachDataException;
import com.nba.core.exception.invalidData.InvalidTeamDataException;
import com.nba.core.exception.notFound.CoachNotFoundException;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CoachServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CoachService coachService;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager entityManager;

    // =========================================================
    // ADD COACH
    // =========================================================

    @Test
    void shouldAddCoachToTeam() {
        Team team = createTeam("Add Coach Team " + unique());

        RequestCoachDto request = new RequestCoachDto(
                "John",
                "Smith",
                new BigDecimal("2500000"),
                team.getId(),
                8,
                3
        );

        ResponseCoachDto result = coachService.addCoach(request);

        assertNotNull(result.id());
        assertEquals("John", result.firstName());
        assertEquals("Smith", result.lastName());
        assertEquals("Coach", result.teamRole());
        assertEquals(team.getName(), result.teamName());
        assertEquals(8, result.yearsExperience());
        assertEquals(3, result.championshipWon());

        assertEquals(
                0,
                new BigDecimal("2500000").compareTo(result.salary())
        );

        entityManager.flush();
        entityManager.clear();

        Coach savedCoach = coachRepository
                .findWithTeamById(result.id())
                .orElseThrow();

        assertEquals("John", savedCoach.getFirstName());
        assertEquals("Smith", savedCoach.getLastName());
        assertEquals(8, savedCoach.getYearsOfExperience());
        assertEquals(3, savedCoach.getChampionshipsWon());

        assertEquals(
                0,
                new BigDecimal("2500000")
                        .compareTo(savedCoach.getSalary())
        );

        assertNotNull(savedCoach.getTeam());

        assertEquals(
                team.getId(),
                savedCoach.getTeam().getId()
        );
    }

    @Test
    void shouldAddCoachAsFreeAgent() {
        RequestCoachDto request = new RequestCoachDto(
                "Free",
                "Agent",
                new BigDecimal("1800000"),
                null,
                4,
                1
        );

        ResponseCoachDto result = coachService.addCoach(request);

        assertNotNull(result.id());
        assertEquals("Free", result.firstName());
        assertEquals("Agent", result.lastName());
        assertEquals("Coach", result.teamRole());
        assertNull(result.teamName());

        entityManager.flush();
        entityManager.clear();

        Coach savedCoach = coachRepository
                .findWithTeamById(result.id())
                .orElseThrow();

        assertNull(savedCoach.getTeam());
    }

    @Test
    void shouldThrowExceptionWhenAddingCoachToMissingTeam() {
        long coachesBefore = coachRepository.count();

        RequestCoachDto request = new RequestCoachDto(
                "Missing",
                "Team",
                new BigDecimal("2000000"),
                999999L,
                5,
                1
        );

        InvalidCoachDataException exception =
                assertThrows(
                        InvalidCoachDataException.class,
                        () -> coachService.addCoach(request)
                );

        assertEquals(
                "Team with id 999999 not found",
                exception.getMessage()
        );

        entityManager.flush();
        entityManager.clear();

        assertEquals(
                coachesBefore,
                coachRepository.count()
        );
    }

    @Test
    void shouldThrowExceptionWhenAddingCoachToFullTeam() {
        Team team = createTeam(
                "Full Coach Team " + unique()
        );

        for (int i = 1; i <= 5; i++) {
            createCoach(
                    "Existing" + i,
                    "Coach",
                    i,
                    team
            );
        }

        RequestCoachDto request = new RequestCoachDto(
                "Sixth",
                "Coach",
                new BigDecimal("2500000"),
                team.getId(),
                6,
                1
        );

        InvalidTeamDataException exception =
                assertThrows(
                        InvalidTeamDataException.class,
                        () -> coachService.addCoach(request)
                );

        assertEquals(
                "The coaching staff is full. Maximum 5 coaches allowed.",
                exception.getMessage()
        );

        entityManager.flush();
        entityManager.clear();

        assertEquals(
                5,
                coachRepository
                        .findAllByTeamId(team.getId())
                        .size()
        );
    }

    // =========================================================
    // PARTIAL UPDATE
    // =========================================================

    @Test
    void shouldPartiallyUpdateCoach() {
        Team team = createTeam(
                "Update Team " + unique()
        );

        Coach coach = createCoach(
                "Old",
                "Name",
                5,
                team
        );

        PatchCoachRequest request = new PatchCoachRequest(
                "Updated",
                "Coach",
                new BigDecimal("3000000"),
                null,
                12,
                7
        );

        ResponseCoachDto result =
                coachService.partialUpdateCoach(
                        coach.getId(),
                        request
                );

        assertEquals(coach.getId(), result.id());
        assertEquals("Updated", result.firstName());
        assertEquals("Coach", result.lastName());
        assertEquals(12, result.yearsExperience());
        assertEquals(7, result.championshipWon());

        assertEquals(
                0,
                new BigDecimal("3000000")
                        .compareTo(result.salary())
        );

        assertEquals(
                team.getName(),
                result.teamName()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals("Updated", updated.getFirstName());
        assertEquals("Coach", updated.getLastName());
        assertEquals(12, updated.getYearsOfExperience());
        assertEquals(7, updated.getChampionshipsWon());

        assertEquals(
                0,
                new BigDecimal("3000000")
                        .compareTo(updated.getSalary())
        );

        assertEquals(
                team.getId(),
                updated.getTeam().getId()
        );
    }

    @Test
    void shouldUpdateOnlyProvidedCoachFields() {
        Team team = createTeam(
                "Partial Fields Team " + unique()
        );

        Coach coach = createCoach(
                "Original",
                "Coach",
                8,
                team
        );

        PatchCoachRequest request = new PatchCoachRequest(
                "Changed",
                null,
                null,
                null,
                null,
                null
        );

        ResponseCoachDto result =
                coachService.partialUpdateCoach(
                        coach.getId(),
                        request
                );

        assertEquals("Changed", result.firstName());
        assertEquals("Coach", result.lastName());
        assertEquals(8, result.yearsExperience());
        assertEquals(2, result.championshipWon());

        assertEquals(
                team.getName(),
                result.teamName()
        );

        assertEquals(
                0,
                new BigDecimal("2500000")
                        .compareTo(result.salary())
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals("Changed", updated.getFirstName());
        assertEquals("Coach", updated.getLastName());
        assertEquals(8, updated.getYearsOfExperience());
        assertEquals(2, updated.getChampionshipsWon());

        assertEquals(
                0,
                new BigDecimal("2500000")
                        .compareTo(updated.getSalary())
        );

        assertEquals(
                team.getId(),
                updated.getTeam().getId()
        );
    }

    @Test
    void shouldMoveCoachToAnotherTeamUsingPatch() {
        Team oldTeam = createTeam(
                "Patch Old Team " + unique()
        );

        Team newTeam = createTeam(
                "Patch New Team " + unique()
        );

        Coach coach = createCoach(
                "Transfer",
                "Coach",
                6,
                oldTeam
        );

        PatchCoachRequest request = new PatchCoachRequest(
                "TransferUpdated",
                "HeadCoach",
                new BigDecimal("3500000"),
                newTeam.getId(),
                15,
                9
        );

        ResponseCoachDto result =
                coachService.partialUpdateCoach(
                        coach.getId(),
                        request
                );

        assertEquals("TransferUpdated", result.firstName());
        assertEquals("HeadCoach", result.lastName());
        assertEquals(15, result.yearsExperience());
        assertEquals(9, result.championshipWon());

        assertEquals(
                0,
                new BigDecimal("3500000")
                        .compareTo(result.salary())
        );

        assertEquals(
                newTeam.getName(),
                result.teamName()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals("TransferUpdated", updated.getFirstName());
        assertEquals("HeadCoach", updated.getLastName());
        assertEquals(15, updated.getYearsOfExperience());
        assertEquals(9, updated.getChampionshipsWon());

        assertEquals(
                0,
                new BigDecimal("3500000")
                        .compareTo(updated.getSalary())
        );

        assertNotNull(updated.getTeam());

        assertEquals(
                newTeam.getId(),
                updated.getTeam().getId()
        );

        assertTrue(
                coachRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .noneMatch(c ->
                                c.getId().equals(coach.getId()))
        );

        assertTrue(
                coachRepository.findAllByTeamId(newTeam.getId())
                        .stream()
                        .anyMatch(c ->
                                c.getId().equals(coach.getId()))
        );
    }

    @Test
    void shouldKeepCoachInSameTeamWhenPatchContainsSameTeamId() {
        Team team = createTeam(
                "Same Patch Team " + unique()
        );

        Coach coach = createCoach(
                "Same",
                "Team",
                5,
                team
        );

        PatchCoachRequest request = new PatchCoachRequest(
                null,
                null,
                null,
                team.getId(),
                null,
                null
        );

        ResponseCoachDto result =
                coachService.partialUpdateCoach(
                        coach.getId(),
                        request
                );

        assertEquals(
                team.getName(),
                result.teamName()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(updated.getTeam());

        assertEquals(
                team.getId(),
                updated.getTeam().getId()
        );
    }

    @Test
    void shouldMoveFreeAgentToTeamUsingPatchAndPreserveOtherFields() {
        String token = unique();

        Team team = createTeam(
                "Patch Free Agent Target " + token
        );

        Coach coach = createCoach(
                "FreePatch_" + token,
                "Coach",
                37,
                null
        );

        BigDecimal salary =
                new BigDecimal("4567890.50");

        coach.setSalary(salary);
        coach.setChampionshipsWon(15);

        entityManager.flush();
        entityManager.clear();

        PatchCoachRequest request =
                new PatchCoachRequest(
                        null,
                        null,
                        null,
                        team.getId(),
                        null,
                        null
                );

        ResponseCoachDto result =
                coachService.partialUpdateCoach(
                        coach.getId(),
                        request
                );

        assertEquals(coach.getId(), result.id());
        assertEquals(
                "FreePatch_" + token,
                result.firstName()
        );
        assertEquals("Coach", result.lastName());
        assertEquals(37, result.yearsExperience());
        assertEquals(15, result.championshipWon());

        assertEquals(
                0,
                salary.compareTo(result.salary())
        );

        assertEquals(
                team.getName(),
                result.teamName()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertEquals(
                "FreePatch_" + token,
                updated.getFirstName()
        );

        assertEquals(
                "Coach",
                updated.getLastName()
        );

        assertEquals(
                37,
                updated.getYearsOfExperience()
        );

        assertEquals(
                15,
                updated.getChampionshipsWon()
        );

        assertEquals(
                0,
                salary.compareTo(updated.getSalary())
        );

        assertNotNull(updated.getTeam());

        assertEquals(
                team.getId(),
                updated.getTeam().getId()
        );
    }

    @Test
    void shouldThrowExceptionWhenUpdatingMissingCoach() {
        PatchCoachRequest request = new PatchCoachRequest(
                "Updated",
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(
                CoachNotFoundException.class,
                () -> coachService.partialUpdateCoach(
                        999999L,
                        request
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackPatchWhenTargetTeamDoesNotExist() {
        Team team = createTeam(
                "Patch Missing Team " + unique()
        );

        Coach coach = createCoach(
                "Patch",
                "Coach",
                5,
                team
        );

        PatchCoachRequest request = new PatchCoachRequest(
                "Changed",
                null,
                null,
                999999L,
                null,
                null
        );

        InvalidCoachDataException exception =
                assertThrows(
                        InvalidCoachDataException.class,
                        () -> coachService.partialUpdateCoach(
                                coach.getId(),
                                request
                        )
                );

        assertEquals(
                "Team with id 999999 not found",
                exception.getMessage()
        );

        entityManager.clear();

        Coach unchanged =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertEquals("Patch", unchanged.getFirstName());
        assertEquals("Coach", unchanged.getLastName());

        assertEquals(
                team.getId(),
                unchanged.getTeam().getId()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackPatchWhenTargetTeamIsFull() {
        Team oldTeam = createTeam(
                "Rollback Old Team " + unique()
        );

        Team fullTeam = createTeam(
                "Rollback Full Team " + unique()
        );

        Coach coach = createCoach(
                "Original",
                "Coach",
                5,
                oldTeam
        );

        for (int i = 1; i <= 5; i++) {
            createCoach(
                    "Existing" + i,
                    "Coach",
                    i,
                    fullTeam
            );
        }

        PatchCoachRequest request = new PatchCoachRequest(
                "ShouldRollback",
                null,
                null,
                fullTeam.getId(),
                null,
                null
        );

        InvalidTeamDataException exception =
                assertThrows(
                        InvalidTeamDataException.class,
                        () -> coachService.partialUpdateCoach(
                                coach.getId(),
                                request
                        )
                );

        assertEquals(
                "The coaching staff is full. Maximum 5 coaches allowed.",
                exception.getMessage()
        );

        entityManager.clear();

        Coach unchanged =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertEquals(
                "Original",
                unchanged.getFirstName()
        );

        assertNotNull(unchanged.getTeam());

        assertEquals(
                oldTeam.getId(),
                unchanged.getTeam().getId()
        );

        assertEquals(
                5,
                coachRepository
                        .findAllByTeamId(fullTeam.getId())
                        .size()
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void shouldDeleteCoach() {
        Coach coach = createCoach(
                "Delete",
                "Coach",
                5,
                null
        );

        Long coachId = coach.getId();

        coachService.deleteCoach(coachId);

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository
                        .findById(coachId)
                        .isEmpty()
        );
    }

    @Test
    void shouldDeleteCoachFromTeam() {
        Team team = createTeam(
                "Delete Team " + unique()
        );

        Coach coach = createCoach(
                "Delete",
                "FromTeam",
                5,
                team
        );

        Long coachId = coach.getId();

        coachService.deleteCoach(coachId);

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository
                        .findById(coachId)
                        .isEmpty()
        );

        assertTrue(
                coachRepository.findAllByTeamId(team.getId())
                        .stream()
                        .noneMatch(c ->
                                c.getId().equals(coachId))
        );
    }

    @Test
    void shouldAllowAnotherCoachAfterDeletingCoachFromTeam() {
        Team team = createTeam(
                "Reuse Coach Slot Team " + unique()
        );

        Coach deletedCoach = createCoach(
                "Deleted",
                "Coach",
                5,
                team
        );

        createCoach("Existing", "Coach", 6, team);
        createCoach("ExistingTwo", "Coach", 7, team);
        createCoach("ExistingThree", "Coach", 8, team);
        createCoach("ExistingFour", "Coach", 9, team);

        coachService.deleteCoach(
                deletedCoach.getId()
        );

        ResponseCoachDto result =
                coachService.addCoach(
                        new RequestCoachDto(
                                "New",
                                "Coach",
                                new BigDecimal("2500000"),
                                team.getId(),
                                10,
                                3
                        )
                );

        assertNotNull(result.id());
        assertEquals("New", result.firstName());
        assertEquals(team.getName(), result.teamName());

        entityManager.flush();
        entityManager.clear();

        assertEquals(
                5,
                coachRepository
                        .findAllByTeamId(team.getId())
                        .size()
        );
    }

    @Test
    void shouldThrowExceptionWhenDeletingMissingCoach() {
        assertThrows(
                CoachNotFoundException.class,
                () -> coachService.deleteCoach(999999L)
        );
    }

    // =========================================================
    // GET COACH
    // =========================================================

    @Test
    void shouldGetCoachById() {
        Team team = createTeam(
                "Get Coach Team " + unique()
        );

        Coach coach = createCoach(
                "John",
                "Smith",
                9,
                team
        );

        ResponseCoachDto result =
                coachService.getCoachById(
                        coach.getId()
                );

        assertEquals(coach.getId(), result.id());
        assertEquals("John", result.firstName());
        assertEquals("Smith", result.lastName());
        assertEquals("Coach", result.teamRole());
        assertEquals(team.getName(), result.teamName());
        assertEquals(9, result.yearsExperience());
        assertEquals(2, result.championshipWon());

        assertEquals(
                0,
                new BigDecimal("2500000")
                        .compareTo(result.salary())
        );
    }

    @Test
    void shouldGetFreeAgentCoachById() {
        Coach coach = createCoach(
                "Free",
                "Coach",
                5,
                null
        );

        ResponseCoachDto result =
                coachService.getCoachById(
                        coach.getId()
                );

        assertEquals(coach.getId(), result.id());
        assertEquals("Free", result.firstName());
        assertEquals("Coach", result.lastName());
        assertNull(result.teamName());
    }

    @Test
    void shouldThrowExceptionWhenGettingMissingCoach() {
        assertThrows(
                CoachNotFoundException.class,
                () -> coachService.getCoachById(999999L)
        );
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Test
    void shouldGetAllCoachesWithPagination() {
        long coachesBefore =
                coachRepository.count();

        Team team = createTeam(
                "Pagination Service Team " + unique()
        );

        createCoach("One", "Coach", 1, team);
        createCoach("Two", "Coach", 2, team);
        createCoach("Three", "Coach", 3, team);

        entityManager.flush();
        entityManager.clear();

        Page<ResponseCoachDto> result =
                coachService.getAllCoaches(
                        PageRequest.of(
                                0,
                                2,
                                Sort.by("id").ascending()
                        )
                );

        assertEquals(
                coachesBefore + 3,
                result.getTotalElements()
        );

        assertEquals(
                2,
                result.getContent().size()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .allMatch(dto ->
                                "Coach".equals(dto.teamRole()))
        );
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void shouldReturnAllCoachesWhenSearchHasNoFilters() {
        long coachesBefore =
                coachRepository.count();

        Team team = createTeam(
                "No Filter Search Team " + unique()
        );

        createCoach(
                "NoFilterOne",
                "Coach",
                1,
                team
        );

        createCoach(
                "NoFilterTwo",
                "Coach",
                2,
                team
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 1000)
                );

        assertEquals(
                coachesBefore + 2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .allMatch(dto ->
                                "Coach".equals(dto.teamRole()))
        );
    }

    @Test
    void shouldReturnAllCoachesWhenSearchFilterIsNull() {
        long coachesBefore =
                coachRepository.count();

        Team team = createTeam(
                "Null Filter Search Team " + unique()
        );

        createCoach(
                "NullFilterOne",
                "Coach",
                40,
                team
        );

        createCoach(
                "NullFilterTwo",
                "Coach",
                41,
                team
        );

        entityManager.flush();
        entityManager.clear();

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        null,
                        PageRequest.of(0, 1000)
                );

        assertNotNull(result);

        assertEquals(
                coachesBefore + 2,
                result.getTotalElements()
        );

        assertEquals(
                coachesBefore + 2,
                result.getContent().size()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .allMatch(dto ->
                                "Coach".equals(dto.teamRole()))
        );
    }

    @Test
    void shouldSearchCoachesByFirstName() {
        String token = unique();

        String matchingFirstName =
                "Alexander_" + token;

        Team team = createTeam(
                "First Name Search Team " + token
        );

        Coach matching = createCoach(
                matchingFirstName,
                "Jackson",
                17,
                team
        );

        createCoach(
                "Michael_" + unique(),
                "Jordan",
                18,
                team
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        matchingFirstName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldSearchCoachesByLastName() {
        String token = unique();

        String matchingLastName =
                "Johnson_" + token;

        Team team = createTeam(
                "Last Name Search Team " + token
        );

        Coach matching = createCoach(
                "John",
                matchingLastName,
                19,
                team
        );

        createCoach(
                "Michael",
                "Smith_" + unique(),
                20,
                team
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        matchingLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldSearchCoachesByFirstNamePartIgnoringCase() {
        String token = unique();

        String fullName =
                "Alexander_" + token;

        Team team = createTeam(
                "First Name Partial Team " + token
        );

        Coach matching = createCoach(
                fullName,
                "Partial",
                50,
                team
        );

        createCoach(
                "Michael_" + unique(),
                "Other",
                51,
                team
        );

        entityManager.flush();
        entityManager.clear();

        String searchPart =
                fullName.substring(0, 10).toUpperCase();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        searchPart,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldSearchCoachesByTeamName() {
        String token = unique();

        String matchingTeamName =
                "GoldenState_" + token;

        Team matchingTeam =
                createTeam(matchingTeamName);

        Team otherTeam =
                createTeam(
                        "Boston_" + unique()
                );

        Coach matching = createCoach(
                "John",
                "Coach",
                21,
                matchingTeam
        );

        createCoach(
                "Mike",
                "Coach",
                22,
                otherTeam
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        null,
                        matchingTeamName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldSearchCoachesByChampionshipsRange() {
        String token = unique();

        String uniqueLastName =
                "ChampionshipRange_" + token;

        Team team = createTeam(
                "Championship Search Team " + token
        );

        Coach matching = createCoach(
                "Winner_" + token,
                uniqueLastName,
                23,
                team
        );

        matching.setChampionshipsWon(1000);

        createCoach(
                "Other_" + token,
                "OtherCoach_" + token,
                24,
                team
        ).setChampionshipsWon(2);

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        999,
                        1001,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldApplyChampionshipBoundariesInclusively() {
        String token = unique();

        String uniqueLastName =
                "ChampionshipBoundary_" + token;

        Team team = createTeam(
                "Championship Boundary Team " + token
        );

        int minValue = 9000;
        int maxValue = 9001;

        Coach minCoach = createCoach(
                "Min_" + token,
                uniqueLastName,
                30,
                team
        );

        minCoach.setChampionshipsWon(minValue);

        Coach maxCoach = createCoach(
                "Max_" + token,
                uniqueLastName,
                31,
                team
        );

        maxCoach.setChampionshipsWon(maxValue);

        createCoach(
                "Outside_" + token,
                "OutsideChampionship_" + token,
                32,
                team
        ).setChampionshipsWon(8999);

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        minValue,
                        maxValue,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(minCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(maxCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesByMinimumChampionshipsOnly() {
        String token = unique();

        String uniqueLastName =
                "MinChampionship_" + token;

        Team team = createTeam(
                "Min Championship Search Team " + token
        );

        int minValue = 9000;

        Coach minCoach = createCoach(
                "Min_" + token,
                uniqueLastName,
                30,
                team
        );

        minCoach.setChampionshipsWon(minValue);

        Coach higherCoach = createCoach(
                "Higher_" + token,
                uniqueLastName,
                31,
                team
        );

        higherCoach.setChampionshipsWon(9001);

        createCoach(
                "Lower_" + token,
                "OutsideMinChampionship_" + token,
                32,
                team
        ).setChampionshipsWon(8999);

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        minValue,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(minCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(higherCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesByMaximumChampionshipsOnly() {
        String token = unique();

        String uniqueLastName =
                "MaxChampionship_" + token;

        Team team = createTeam(
                "Max Championship Search Team " + token
        );

        int maxValue = 9001;

        Coach lowerCoach = createCoach(
                "Lower_" + token,
                uniqueLastName,
                30,
                team
        );

        lowerCoach.setChampionshipsWon(9000);

        Coach maxCoach = createCoach(
                "Max_" + token,
                uniqueLastName,
                31,
                team
        );

        maxCoach.setChampionshipsWon(maxValue);

        createCoach(
                "Higher_" + token,
                "OutsideMaxChampionship_" + token,
                32,
                team
        ).setChampionshipsWon(9002);

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        maxValue,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(lowerCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(maxCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesByExperienceRange() {
        String token = unique();

        String uniqueLastName =
                "ExperienceRange_" + token;

        Team team = createTeam(
                "Experience Search Team " + token
        );

        Coach matching = createCoach(
                "Experienced_" + token,
                uniqueLastName,
                1000,
                team
        );

        createCoach(
                "Junior_" + token,
                "OutsideExperience_" + token,
                2,
                team
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        999,
                        1001,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldApplyExperienceBoundariesInclusively() {
        String token = unique();

        String uniqueLastName =
                "ExperienceBoundary_" + token;

        Team team = createTeam(
                "Experience Boundary Team " + token
        );

        int minExperience = 9000;
        int maxExperience = 9001;

        Coach minCoach = createCoach(
                "Min_" + token,
                uniqueLastName,
                minExperience,
                team
        );

        Coach maxCoach = createCoach(
                "Max_" + token,
                uniqueLastName,
                maxExperience,
                team
        );

        createCoach(
                "Outside_" + token,
                "OutsideExperienceBoundary_" + token,
                8999,
                team
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        minExperience,
                        maxExperience,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(minCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(maxCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesByMinimumExperienceOnly() {
        String token = unique();

        String uniqueLastName =
                "MinExperience_" + token;

        Team team = createTeam(
                "Min Experience Search Team " + token
        );

        int minExperience = 9000;

        Coach minCoach = createCoach(
                "Min_" + token,
                uniqueLastName,
                minExperience,
                team
        );

        Coach higherCoach = createCoach(
                "Higher_" + token,
                uniqueLastName,
                9001,
                team
        );

        createCoach(
                "Lower_" + token,
                "OutsideMinExperience_" + token,
                8999,
                team
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        minExperience,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(minCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(higherCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesByMaximumExperienceOnly() {
        String token = unique();

        String uniqueLastName =
                "MaxExperience_" + token;

        Team team = createTeam(
                "Max Experience Search Team " + token
        );

        int maxExperience = 9001;

        Coach lowerCoach = createCoach(
                "Lower_" + token,
                uniqueLastName,
                9000,
                team
        );

        Coach maxCoach = createCoach(
                "Max_" + token,
                uniqueLastName,
                maxExperience,
                team
        );

        createCoach(
                "Higher_" + token,
                "OutsideMaxExperience_" + token,
                9002,
                team
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        maxExperience,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(lowerCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(maxCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesBySalaryRange() {
        String token = unique();

        String uniqueLastName =
                "SalaryRange_" + token;

        Team team = createTeam(
                "Salary Search Team " + token
        );

        Coach matching = createCoach(
                "High_" + token,
                uniqueLastName,
                25,
                team
        );

        matching.setSalary(
                new BigDecimal("9876543.21")
        );

        createCoach(
                "Low_" + token,
                "OutsideSalaryRange_" + token,
                26,
                team
        ).setSalary(
                new BigDecimal("1234.56")
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("9876543.20"),
                        new BigDecimal("9876543.22")
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldApplySalaryBoundariesInclusively() {
        String token = unique();

        String uniqueLastName =
                "SalaryBoundary_" + token;

        Team team = createTeam(
                "Salary Boundary Team " + token
        );

        BigDecimal minSalary =
                new BigDecimal("900000000.00");

        BigDecimal maxSalary =
                new BigDecimal("900000001.00");

        Coach minCoach = createCoach(
                "Min_" + token,
                uniqueLastName,
                30,
                team
        );

        minCoach.setSalary(minSalary);

        Coach maxCoach = createCoach(
                "Max_" + token,
                uniqueLastName,
                31,
                team
        );

        maxCoach.setSalary(maxSalary);

        createCoach(
                "Outside_" + token,
                "OutsideSalaryBoundary_" + token,
                32,
                team
        ).setSalary(
                new BigDecimal("899999999.00")
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        minSalary,
                        maxSalary
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(minCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(maxCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesByMinimumSalaryOnly() {
        String token = unique();

        String uniqueLastName =
                "MinSalary_" + token;

        Team team = createTeam(
                "Min Salary Search Team " + token
        );

        BigDecimal minSalary =
                new BigDecimal("900000000.00");

        Coach minCoach = createCoach(
                "Min_" + token,
                uniqueLastName,
                30,
                team
        );

        minCoach.setSalary(minSalary);

        Coach higherCoach = createCoach(
                "Higher_" + token,
                uniqueLastName,
                31,
                team
        );

        higherCoach.setSalary(
                new BigDecimal("900000001.00")
        );

        createCoach(
                "Lower_" + token,
                "OutsideMinSalary_" + token,
                32,
                team
        ).setSalary(
                new BigDecimal("899999999.00")
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        minSalary,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(minCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(higherCoach.getId()))
        );
    }

    @Test
    void shouldSearchCoachesByMaximumSalaryOnly() {
        String token = unique();

        String uniqueLastName =
                "MaxSalary_" + token;

        Team team = createTeam(
                "Max Salary Search Team " + token
        );

        BigDecimal maxSalary =
                new BigDecimal("900000001.00");

        Coach lowerCoach = createCoach(
                "Lower_" + token,
                uniqueLastName,
                30,
                team
        );

        lowerCoach.setSalary(
                new BigDecimal("900000000.00")
        );

        Coach maxCoach = createCoach(
                "Max_" + token,
                uniqueLastName,
                31,
                team
        );

        maxCoach.setSalary(maxSalary);

        createCoach(
                "Higher_" + token,
                "OutsideMaxSalary_" + token,
                32,
                team
        ).setSalary(
                new BigDecimal("900000002.00")
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        null,
                        uniqueLastName.toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        maxSalary
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                2,
                result.getTotalElements()
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(lowerCoach.getId()))
        );

        assertTrue(
                result.getContent()
                        .stream()
                        .anyMatch(dto ->
                                dto.id().equals(maxCoach.getId()))
        );
    }

    @Test
    void shouldCombineMultipleSearchFilters() {
        String token = unique();

        String matchingFirstName =
                "Mike_" + token;

        String matchingLastName =
                "Jackson_" + token;

        String teamName =
                "CombinedTeam_" + token;

        Team matchingTeam =
                createTeam(teamName);

        Coach matching = createCoach(
                matchingFirstName,
                matchingLastName,
                777,
                matchingTeam
        );

        matching.setSalary(
                new BigDecimal("8765432.10")
        );

        matching.setChampionshipsWon(777);

        Coach wrongFirstName = createCoach(
                "John_" + unique(),
                matchingLastName,
                777,
                matchingTeam
        );

        wrongFirstName.setSalary(
                new BigDecimal("8765432.10")
        );

        wrongFirstName.setChampionshipsWon(777);

        createCoach(
                matchingFirstName,
                "Other_" + unique(),
                777,
                createTeam(
                        "OtherTeam_" + unique()
                )
        );

        entityManager.flush();
        entityManager.clear();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        matchingFirstName.toLowerCase(),
                        matchingLastName.toLowerCase(),
                        teamName.toLowerCase(),
                        777,
                        777,
                        777,
                        777,
                        new BigDecimal("8765432.09"),
                        new BigDecimal("8765432.11")
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                matching.getId(),
                result.getContent().get(0).id()
        );
    }

    @Test
    void shouldReturnEmptyResultWhenNoCoachMatchesSearch() {
        String uniqueSearchValue =
                "NO_MATCH_" + unique();

        CoachSearchFilter filter =
                new CoachSearchFilter(
                        uniqueSearchValue,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseCoachDto> result =
                coachService.searchCoaches(
                        filter,
                        PageRequest.of(0, 20)
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    // =========================================================
    // COLLEAGUES
    // =========================================================

    @Test
    void shouldGetCoachColleagues() {
        Team team = createTeam(
                "Colleagues Team " + unique()
        );

        Coach mainCoach = createCoach(
                "Main",
                "Coach",
                10,
                team
        );

        Coach colleague1 = createCoach(
                "First",
                "Assistant",
                5,
                team
        );

        Coach colleague2 = createCoach(
                "Second",
                "Assistant",
                6,
                team
        );

        TeamGroupResponse result =
                coachService.getColleaguesByCoachId(
                        mainCoach.getId()
                );

        assertEquals(
                team.getName(),
                result.teamName()
        );

        assertEquals(
                "COLLEAGUES OF Main Coach",
                result.title()
        );

        assertEquals(
                2,
                result.members().size()
        );

        assertTrue(
                result.members()
                        .stream()
                        .anyMatch(member ->
                                member.id().equals(colleague1.getId())
                                        && member.fullName()
                                        .equals("First Assistant"))
        );

        assertTrue(
                result.members()
                        .stream()
                        .anyMatch(member ->
                                member.id().equals(colleague2.getId())
                                        && member.fullName()
                                        .equals("Second Assistant"))
        );

        assertFalse(
                result.members()
                        .stream()
                        .anyMatch(member ->
                                member.id().equals(mainCoach.getId()))
        );
    }

    @Test
    void shouldReturnEmptyColleaguesWhenCoachIsOnlyMemberOfTeam() {
        Team team = createTeam(
                "Only Coach Team " + unique()
        );

        Coach coach = createCoach(
                "Only",
                "Coach",
                5,
                team
        );

        TeamGroupResponse result =
                coachService.getColleaguesByCoachId(
                        coach.getId()
                );

        assertEquals(
                team.getName(),
                result.teamName()
        );

        assertEquals(
                "COLLEAGUES OF Only Coach",
                result.title()
        );

        assertNotNull(result.members());
        assertTrue(result.members().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenFreeAgentRequestsColleagues() {
        Coach freeAgent = createCoach(
                "Free",
                "Agent",
                5,
                null
        );

        InvalidCoachDataException exception =
                assertThrows(
                        InvalidCoachDataException.class,
                        () -> coachService.getColleaguesByCoachId(
                                freeAgent.getId()
                        )
                );

        assertEquals(
                "Coach with id " + freeAgent.getId()
                        + " doesn't work in any team and has no colleagues",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenGettingColleaguesForMissingCoach() {
        assertThrows(
                CoachNotFoundException.class,
                () -> coachService.getColleaguesByCoachId(
                        999999L
                )
        );
    }

    // =========================================================
    // CHANGE TEAM
    // =========================================================

    @Test
    void shouldChangeCoachTeam() {
        Team oldTeam = createTeam(
                "Old Coach Team " + unique()
        );

        Team newTeam = createTeam(
                "New Coach Team " + unique()
        );

        Coach coach = createCoach(
                "Trade",
                "Coach",
                7,
                oldTeam
        );

        TeamTransferResponse result =
                coachService.changeCoachTeam(
                        coach.getId(),
                        newTeam.getId()
                );

        assertEquals(
                coach.getId(),
                result.memberId()
        );

        assertEquals(
                "Trade Coach",
                result.memberFullName()
        );

        assertEquals(
                "COACH",
                result.memberRole()
        );

        assertEquals(
                oldTeam.getName(),
                result.oldTeamName()
        );

        assertEquals(
                newTeam.getId(),
                result.newTeamId()
        );

        assertEquals(
                newTeam.getName(),
                result.newTeamName()
        );

        assertTrue(
                result.message()
                        .contains(
                                "Successfully traded COACH Trade Coach"
                        )
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertEquals(
                newTeam.getId(),
                updated.getTeam().getId()
        );

        assertTrue(
                coachRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .noneMatch(c ->
                                c.getId().equals(coach.getId()))
        );

        assertTrue(
                coachRepository.findAllByTeamId(newTeam.getId())
                        .stream()
                        .anyMatch(c ->
                                c.getId().equals(coach.getId()))
        );
    }

    @Test
    void shouldPreserveCoachDataWhenChangingTeam() {
        Team oldTeam = createTeam(
                "Preserve Old Team " + unique()
        );

        Team newTeam = createTeam(
                "Preserve New Team " + unique()
        );

        Coach coach = createCoach(
                "Preserved",
                "Coach",
                14,
                oldTeam
        );

        coach.setSalary(
                new BigDecimal("4200000.50")
        );

        coach.setChampionshipsWon(11);

        entityManager.flush();

        TeamTransferResponse result =
                coachService.changeCoachTeam(
                        coach.getId(),
                        newTeam.getId()
                );

        assertEquals(
                newTeam.getName(),
                result.newTeamName()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertEquals(
                "Preserved",
                updated.getFirstName()
        );

        assertEquals(
                "Coach",
                updated.getLastName()
        );

        assertEquals(
                14,
                updated.getYearsOfExperience()
        );

        assertEquals(
                11,
                updated.getChampionshipsWon()
        );

        assertEquals(
                0,
                new BigDecimal("4200000.50")
                        .compareTo(updated.getSalary())
        );

        assertEquals(
                newTeam.getId(),
                updated.getTeam().getId()
        );
    }

    @Test
    void shouldMoveFreeAgentToTeam() {
        Team newTeam = createTeam(
                "Free Agent Target " + unique()
        );

        Coach coach = createCoach(
                "Free",
                "Coach",
                5,
                null
        );

        TeamTransferResponse result =
                coachService.changeCoachTeam(
                        coach.getId(),
                        newTeam.getId()
                );

        assertEquals(
                "Free Agent",
                result.oldTeamName()
        );

        assertEquals(
                newTeam.getId(),
                result.newTeamId()
        );

        assertEquals(
                newTeam.getName(),
                result.newTeamName()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertNotNull(updated.getTeam());

        assertEquals(
                newTeam.getId(),
                updated.getTeam().getId()
        );
    }

    @Test
    void shouldMakeCoachFreeAgent() {
        Team team = createTeam(
                "Remove Coach Team " + unique()
        );

        Coach coach = createCoach(
                "Remove",
                "Coach",
                6,
                team
        );

        TeamTransferResponse result =
                coachService.changeCoachTeam(
                        coach.getId(),
                        null
                );

        assertEquals(
                team.getName(),
                result.oldTeamName()
        );

        assertNull(result.newTeamId());
        assertEquals(
                "Free Agent",
                result.newTeamName()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updated =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertNull(updated.getTeam());

        assertTrue(
                coachRepository.findAllByTeamId(team.getId())
                        .stream()
                        .noneMatch(c ->
                                c.getId().equals(coach.getId()))
        );
    }

    @Test
    void shouldThrowExceptionWhenCoachMovesToSameTeam() {
        Team team = createTeam(
                "Same Team " + unique()
        );

        Coach coach = createCoach(
                "Same",
                "Coach",
                5,
                team
        );

        InvalidCoachDataException exception =
                assertThrows(
                        InvalidCoachDataException.class,
                        () -> coachService.changeCoachTeam(
                                coach.getId(),
                                team.getId()
                        )
                );

        assertEquals(
                "Coach is already a team member of this team",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenFreeAgentMovesToFreeAgent() {
        Coach coach = createCoach(
                "Already",
                "Free",
                5,
                null
        );

        InvalidCoachDataException exception =
                assertThrows(
                        InvalidCoachDataException.class,
                        () -> coachService.changeCoachTeam(
                                coach.getId(),
                                null
                        )
                );

        assertEquals(
                "Coach is already a free agent",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenChangingToMissingTeam() {
        Coach coach = createCoach(
                "Missing",
                "Target",
                5,
                null
        );

        InvalidCoachDataException exception =
                assertThrows(
                        InvalidCoachDataException.class,
                        () -> coachService.changeCoachTeam(
                                coach.getId(),
                                999999L
                        )
                );

        assertEquals(
                "Team with id 999999 not found",
                exception.getMessage()
        );

        entityManager.flush();
        entityManager.clear();

        Coach unchanged =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertNull(unchanged.getTeam());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackChangeTeamWhenTargetTeamIsFull() {
        Team oldTeam = createTeam(
                "Trade Rollback Old " + unique()
        );

        Team fullTeam = createTeam(
                "Trade Rollback Full " + unique()
        );

        Coach coach = createCoach(
                "Rollback",
                "Coach",
                5,
                oldTeam
        );

        for (int i = 1; i <= 5; i++) {
            createCoach(
                    "Existing" + i,
                    "Coach",
                    i,
                    fullTeam
            );
        }

        InvalidTeamDataException exception =
                assertThrows(
                        InvalidTeamDataException.class,
                        () -> coachService.changeCoachTeam(
                                coach.getId(),
                                fullTeam.getId()
                        )
                );

        assertEquals(
                "The coaching staff is full. Maximum 5 coaches allowed.",
                exception.getMessage()
        );

        entityManager.clear();

        Coach unchanged =
                coachRepository
                        .findWithTeamById(coach.getId())
                        .orElseThrow();

        assertNotNull(unchanged.getTeam());

        assertEquals(
                oldTeam.getId(),
                unchanged.getTeam().getId()
        );

        assertTrue(
                coachRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .anyMatch(c ->
                                c.getId().equals(coach.getId()))
        );

        assertEquals(
                5,
                coachRepository
                        .findAllByTeamId(fullTeam.getId())
                        .size()
        );
    }

    @Test
    void shouldThrowExceptionWhenChangingMissingCoach() {
        assertThrows(
                CoachNotFoundException.class,
                () -> coachService.changeCoachTeam(
                        999999L,
                        null
                )
        );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Team createTeam(String name) {
        Team team = Team.builder()
                .name(name)
                .creationYear(2020)
                .championshipTitleCount(0)
                .build();

        return teamRepository.save(team);
    }

    private Coach createCoach(
            String firstName,
            String lastName,
            Integer yearsOfExperience,
            Team team
    ) {
        Coach coach = Coach.builder()
                .firstName(firstName)
                .lastName(lastName)
                .salary(new BigDecimal("2500000"))
                .yearsOfExperience(yearsOfExperience)
                .championshipsWon(2)
                .build();

        if (team != null) {
            team.addTeamMember(coach);
        }

        return coachRepository.save(coach);
    }

    private String unique() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}

