package com.nba.coach;

import com.nba.AbstractIntegrationTest;
import com.nba.core.exception.notFound.CoachNotFoundException;
import com.nba.player.Player;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CoachRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager entityManager;

    // =========================================================
    // findAll(Pageable)
    // =========================================================

    @Test
    void shouldFindAllCoachesWithPagination() {
        long coachesBefore = coachRepository.count();

        Team team = createTeam("Pagination Team " + unique());

        createCoach("John", "Coach", 5, team);
        createCoach("Mike", "Coach", 8, team);
        createCoach("Alex", "Coach", 10, team);

        entityManager.flush();
        entityManager.clear();

        long expectedTotal = coachesBefore + 3;

        Page<Coach> result = coachRepository.findAll(
                PageRequest.of(
                        0,
                        2,
                        Sort.by("id").ascending()
                )
        );

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedTotal, result.getTotalElements());
        assertEquals(
                (int) Math.ceil((double) expectedTotal / 2),
                result.getTotalPages()
        );

        assertTrue(result.getContent()
                .stream()
                .allMatch(coach -> coach instanceof Coach));
    }

    @Test
    void shouldReturnEmptyPageWhenRequestedPageIsOutOfBounds() {
        Team team = createTeam("Out Of Bounds Team " + unique());

        createCoach("John", "Coach", 5, team);

        entityManager.flush();
        entityManager.clear();

        long total = coachRepository.count();

        int pageSize = 10;
        int outOfBoundsPage = (int) Math.ceil((double) total / pageSize);

        Page<Coach> result = coachRepository.findAll(
                PageRequest.of(
                        outOfBoundsPage,
                        pageSize,
                        Sort.by("id").ascending()
                )
        );

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(total, result.getTotalElements());
    }

    @Test
    void shouldSortCoachesByIdAscending() {
        Team team = createTeam("Sort Team " + unique());

        Coach coach1 = createCoach("First", "Coach", 3, team);
        Coach coach2 = createCoach("Second", "Coach", 5, team);
        Coach coach3 = createCoach("Third", "Coach", 7, team);

        entityManager.flush();
        entityManager.clear();

        Page<Coach> result = coachRepository.findAll(
                PageRequest.of(
                        0,
                        1000,
                        Sort.by("id").ascending()
                )
        );

        List<Long> ids = result.getContent()
                .stream()
                .map(Coach::getId)
                .toList();

        assertTrue(ids.indexOf(coach1.getId()) < ids.indexOf(coach2.getId()));
        assertTrue(ids.indexOf(coach2.getId()) < ids.indexOf(coach3.getId()));
    }

    // =========================================================
    // findAllByTeamId(Long teamId)
    // =========================================================

    @Test
    void shouldFindAllCoachesByTeamId() {
        Team firstTeam = createTeam("First Team " + unique());
        Team secondTeam = createTeam("Second Team " + unique());

        Coach coach1 = createCoach(
                "John",
                "First",
                5,
                firstTeam
        );

        Coach coach2 = createCoach(
                "Mike",
                "Second",
                7,
                firstTeam
        );

        createCoach(
                "Alex",
                "Other",
                10,
                secondTeam
        );

        entityManager.flush();
        entityManager.clear();

        List<Coach> result = coachRepository.findAllByTeamId(firstTeam.getId());

        assertEquals(2, result.size());

        assertTrue(result.stream()
                .map(Coach::getId)
                .toList()
                .containsAll(List.of(
                        coach1.getId(),
                        coach2.getId()
                )));

        assertTrue(result.stream()
                .allMatch(coach ->
                        coach.getTeam() != null
                                && coach.getTeam().getId().equals(firstTeam.getId())));
    }

    @Test
    void shouldReturnEmptyListWhenTeamHasNoCoaches() {
        Team team = createTeam("Empty Team " + unique());

        entityManager.flush();
        entityManager.clear();

        List<Coach> result = coachRepository.findAllByTeamId(team.getId());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenTeamDoesNotExist() {
        List<Coach> result = coachRepository.findAllByTeamId(999999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnOnlyCoachesFromRequestedTeam() {
        Team firstTeam = createTeam("Requested Team " + unique());
        Team secondTeam = createTeam("Another Team " + unique());

        Coach firstCoach = createCoach(
                "First",
                "Coach",
                3,
                firstTeam
        );

        createCoach(
                "Second",
                "Coach",
                6,
                secondTeam
        );

        Player player = Player.builder()
                .firstName("First")
                .lastName("Player")
                .salary(new BigDecimal("2000000"))
                .championshipsWon(1)
                .rating(90)
                .build();

        firstTeam.addTeamMember(player);

        entityManager.flush();
        entityManager.clear();

        List<Coach> result = coachRepository.findAllByTeamId(firstTeam.getId());

        assertEquals(1, result.size());

        Coach foundCoach = result.get(0);

        assertEquals(firstCoach.getId(), foundCoach.getId());
        assertEquals(firstTeam.getId(), foundCoach.getTeam().getId());

        assertFalse(result.stream()
                .anyMatch(coach ->
                        coach.getId().equals(player.getId())));
    }

    @Test
    void shouldLoadTeamWhenFindingCoachesByTeamId() {
        Team team = createTeam("Entity Graph List Team " + unique());

        Coach coach = createCoach(
                "Graph",
                "Coach",
                8,
                team
        );

        entityManager.flush();
        entityManager.clear();

        List<Coach> result = coachRepository.findAllByTeamId(team.getId());

        assertEquals(1, result.size());

        Coach foundCoach = result.get(0);

        assertEquals(coach.getId(), foundCoach.getId());
        assertNotNull(foundCoach.getTeam());
        assertTrue(Hibernate.isInitialized(foundCoach.getTeam()));
        assertEquals(team.getId(), foundCoach.getTeam().getId());
        assertEquals(team.getName(), foundCoach.getTeam().getName());
    }

    // =========================================================
    // findWithTeamById(Long coachId)
    // =========================================================

    @Test
    void shouldFindCoachWithTeamById() {
        Team team = createTeam("Coach Team " + unique());

        Coach coach = createCoach(
                "John",
                "Smith",
                6,
                team
        );

        entityManager.flush();
        entityManager.clear();

        Coach result = coachRepository.findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals(coach.getId(), result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals(6, result.getYearsOfExperience());

        assertNotNull(result.getTeam());
        assertEquals(team.getId(), result.getTeam().getId());
        assertEquals(team.getName(), result.getTeam().getName());
    }

    @Test
    void shouldLoadTeamUsingEntityGraph() {
        Team team = createTeam("Entity Graph Team " + unique());

        Coach coach = createCoach(
                "Entity",
                "Graph",
                7,
                team
        );

        entityManager.flush();
        entityManager.clear();

        Coach result = coachRepository.findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(result.getTeam());
        assertTrue(Hibernate.isInitialized(result.getTeam()));

        assertEquals(team.getId(), result.getTeam().getId());
        assertEquals(team.getName(), result.getTeam().getName());
    }

    @Test
    void shouldFindFreeAgentCoachWithNullTeam() {
        Coach coach = createCoach(
                "Free",
                "Agent",
                4,
                null
        );

        entityManager.flush();
        entityManager.clear();

        Coach result = coachRepository.findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals(coach.getId(), result.getId());
        assertNull(result.getTeam());
    }

    @Test
    void shouldReturnEmptyWhenCoachWithGivenIdDoesNotExist() {
        assertTrue(
                coachRepository.findWithTeamById(999999L).isEmpty()
        );
    }

    // =========================================================
    // findByIdAndTeamId(Long coachId, Long teamId)
    // =========================================================

    @Test
    void shouldFindCoachByIdAndTeamId() {
        Team team = createTeam("Find By Team " + unique());

        Coach coach = createCoach(
                "John",
                "Coach",
                4,
                team
        );

        entityManager.flush();
        entityManager.clear();

        Coach result = coachRepository.findByIdAndTeamId(
                coach.getId(),
                team.getId()
        ).orElseThrow();

        assertEquals(coach.getId(), result.getId());
        assertEquals(team.getId(), result.getTeam().getId());
    }

    @Test
    void shouldLoadTeamWhenFindingCoachByIdAndTeamId() {
        Team team = createTeam("Graph By Team " + unique());

        Coach coach = createCoach(
                "Graph",
                "Team",
                5,
                team
        );

        entityManager.flush();
        entityManager.clear();

        Coach result = coachRepository.findByIdAndTeamId(
                coach.getId(),
                team.getId()
        ).orElseThrow();

        assertNotNull(result.getTeam());
        assertTrue(Hibernate.isInitialized(result.getTeam()));
        assertEquals(team.getId(), result.getTeam().getId());
    }

    @Test
    void shouldReturnEmptyWhenCoachBelongsToAnotherTeam() {
        Team firstTeam = createTeam("First Search Team " + unique());
        Team secondTeam = createTeam("Second Search Team " + unique());

        Coach coach = createCoach(
                "John",
                "Coach",
                4,
                firstTeam
        );

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository.findByIdAndTeamId(
                        coach.getId(),
                        secondTeam.getId()
                ).isEmpty()
        );
    }

    @Test
    void shouldReturnEmptyWhenCoachDoesNotExistForGivenTeam() {
        Team team = createTeam("Missing Coach Team " + unique());

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository.findByIdAndTeamId(
                        999999L,
                        team.getId()
                ).isEmpty()
        );
    }

    @Test
    void shouldReturnEmptyWhenFreeAgentCoachIsSearchedByTeamId() {
        Coach coach = createCoach(
                "Free",
                "Coach",
                5,
                null
        );

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository.findByIdAndTeamId(
                        coach.getId(),
                        999999L
                ).isEmpty()
        );
    }

    // =========================================================
    // getCoachByIdOrThrow404(Long coachId)
    // =========================================================

    @Test
    void shouldReturnCoachFromGetCoachByIdOrThrow404() {
        Team team = createTeam("Throw Test Team " + unique());

        Coach coach = createCoach(
                "John",
                "Returned",
                9,
                team
        );

        entityManager.flush();
        entityManager.clear();

        Coach result = coachRepository.getCoachByIdOrThrow404(coach.getId());

        assertNotNull(result);
        assertEquals(coach.getId(), result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals("Returned", result.getLastName());
        assertEquals(9, result.getYearsOfExperience());

        assertNotNull(result.getTeam());
        assertEquals(team.getId(), result.getTeam().getId());
    }

    @Test
    void shouldThrowCoachNotFoundExceptionWhenCoachDoesNotExist() {
        CoachNotFoundException exception = assertThrows(
                CoachNotFoundException.class,
                () -> coachRepository.getCoachByIdOrThrow404(999999L)
        );

        assertEquals(
                "Coach with id 999999 not found",
                exception.getMessage()
        );
    }

    // =========================================================
    // Inheritance / discriminator
    // =========================================================

    @Test
    void shouldFindOnlyCoachesAndNotPlayers() {
        Team team = createTeam("Inheritance Team " + unique());

        Coach coach = createCoach(
                "John",
                "Coach",
                5,
                team
        );

        Player player = Player.builder()
                .firstName("John")
                .lastName("Player")
                .salary(new BigDecimal("2000000"))
                .championshipsWon(1)
                .rating(90)
                .build();

        team.addTeamMember(player);

        entityManager.flush();
        entityManager.clear();

        Page<Coach> result = coachRepository.findAll(
                PageRequest.of(
                        0,
                        1000,
                        Sort.by("id").ascending()
                )
        );

        assertTrue(result.getTotalElements() >= 1);

        assertTrue(result.getContent()
                .stream()
                .allMatch(foundCoach -> foundCoach instanceof Coach));

        assertTrue(result.getContent()
                .stream()
                .anyMatch(foundCoach ->
                        foundCoach.getId().equals(coach.getId())));

        assertFalse(result.getContent()
                .stream()
                .anyMatch(foundCoach ->
                        foundCoach.getId().equals(player.getId())));
    }

    // =========================================================
    // Helpers
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
        return java.util.UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}

