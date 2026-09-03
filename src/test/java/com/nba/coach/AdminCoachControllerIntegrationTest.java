package com.nba.coach;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminCoachControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // CREATE COACH
    // =========================================================

    @Test
    void shouldCreateCoachAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Create Coach Team " + unique());

        RequestCoachDto request = new RequestCoachDto(
                "John",
                "Create",
                new BigDecimal("4500000"),
                team.getId(),
                15,
                3
        );

        String response = mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Create")))
                .andExpect(jsonPath("$.teamName", is(team.getName())))
                .andExpect(jsonPath("$.yearsExperience", is(15)))
                .andExpect(jsonPath("$.championshipWon", is(3)))
                .andExpect(jsonPath("$.salary", is(4500000)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseCoachDto result =
                objectMapper.readValue(response, ResponseCoachDto.class);

        assertNotNull(result.id());

        entityManager.flush();
        entityManager.clear();

        Coach savedCoach = coachRepository
                .findWithTeamById(result.id())
                .orElseThrow();

        assertEquals("John", savedCoach.getFirstName());
        assertEquals("Create", savedCoach.getLastName());
        assertEquals(team.getId(), savedCoach.getTeam().getId());
        assertEquals(15, savedCoach.getYearsOfExperience());
        assertEquals(3, savedCoach.getChampionshipsWon());
        assertEquals(
                0,
                new BigDecimal("4500000").compareTo(savedCoach.getSalary())
        );
    }

    @Test
    void shouldCreateCoachAsFreeAgent() throws Exception {
        authenticateAsAdmin();

        RequestCoachDto request = new RequestCoachDto(
                "Free",
                "Agent",
                new BigDecimal("2500000"),
                null,
                8,
                1
        );

        String response = mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("Free")))
                .andExpect(jsonPath("$.lastName", is("Agent")))
                .andExpect(jsonPath("$.teamName").doesNotExist())
                .andExpect(jsonPath("$.yearsExperience", is(8)))
                .andExpect(jsonPath("$.championshipWon", is(1)))
                .andExpect(jsonPath("$.salary", is(2500000)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseCoachDto result =
                objectMapper.readValue(response, ResponseCoachDto.class);

        assertNotNull(result.id());

        entityManager.flush();
        entityManager.clear();

        Coach savedCoach = coachRepository
                .findWithTeamById(result.id())
                .orElseThrow();

        assertNull(savedCoach.getTeam());
    }

    @Test
    void shouldReturnBadRequestWhenCreatingCoachForMissingTeam()
            throws Exception {

        authenticateAsAdmin();

        RequestCoachDto request = new RequestCoachDto(
                "Missing",
                "Team",
                new BigDecimal("3000000"),
                999999L,
                10,
                1
        );

        mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreatingCoachInFullTeam()
            throws Exception {

        authenticateAsAdmin();

        Team team = createTeam("Full Coach Team " + unique());

        for (int i = 1; i <= 5; i++) {
            createCoach(
                    "Existing" + i,
                    "Coach",
                    new BigDecimal("2000000"),
                    10,
                    1,
                    team
            );
        }

        RequestCoachDto request = new RequestCoachDto(
                "Sixth",
                "Coach",
                new BigDecimal("2500000"),
                team.getId(),
                12,
                2
        );

        mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("The coaching staff is full. Maximum 5 coaches allowed.")));
    }

    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToCreateCoach()
            throws Exception {

        authenticateAsUser();

        RequestCoachDto request = new RequestCoachDto(
                "John",
                "User",
                new BigDecimal("2000000"),
                null,
                10,
                0
        );

        mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWhenUnauthenticatedUserTriesToCreateCoach()
            throws Exception {

        RequestCoachDto request = new RequestCoachDto(
                "John",
                "Anonymous",
                new BigDecimal("2000000"),
                null,
                10,
                0
        );

        mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBadRequestWhenCreatingCoachWithInvalidData()
            throws Exception {

        authenticateAsAdmin();

        RequestCoachDto request = new RequestCoachDto(
                "",
                "",
                BigDecimal.ZERO,
                null,
                -1,
                -1
        );

        mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreateCoachRequestIsMalformedJson()
            throws Exception {

        authenticateAsAdmin();

        mockMvc.perform(
                        post("/api/admin/coaches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "firstName": "John",
                                            "lastName":
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // PATCH COACH
    // =========================================================

    @Test
    void shouldPartiallyUpdateCoachAsAdmin() throws Exception {
        authenticateAsAdmin();

        Coach coach = createCoach(
                "Old",
                "Coach",
                new BigDecimal("3000000"),
                10,
                1,
                null
        );

        PatchCoachRequest request = new PatchCoachRequest(
                "Updated",
                "Coach",
                new BigDecimal("5000000"),
                null,
                20,
                5
        );

        String response = mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.lastName", is("Coach")))
                .andExpect(jsonPath("$.yearsExperience", is(20)))
                .andExpect(jsonPath("$.championshipWon", is(5)))
                .andExpect(jsonPath("$.salary", is(5000000)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseCoachDto result =
                objectMapper.readValue(response, ResponseCoachDto.class);

        assertEquals(coach.getId(), result.id());

        entityManager.flush();
        entityManager.clear();

        Coach updatedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals("Updated", updatedCoach.getFirstName());
        assertEquals("Coach", updatedCoach.getLastName());
        assertEquals(20, updatedCoach.getYearsOfExperience());
        assertEquals(5, updatedCoach.getChampionshipsWon());
        assertEquals(
                0,
                new BigDecimal("5000000")
                        .compareTo(updatedCoach.getSalary())
        );
    }

    @Test
    void shouldPartiallyUpdateOnlyProvidedFields()
            throws Exception {

        authenticateAsAdmin();

        Team team = createTeam(
                "Partial Update Coach Team " + unique()
        );

        Coach coach = createCoach(
                "Original",
                "Coach",
                new BigDecimal("3500000"),
                15,
                4,
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

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Changed")))
                .andExpect(jsonPath("$.lastName", is("Coach")))
                .andExpect(jsonPath("$.teamName", is(team.getName())))
                .andExpect(jsonPath("$.yearsExperience", is(15)))
                .andExpect(jsonPath("$.championshipWon", is(4)))
                .andExpect(jsonPath("$.salary", is(3500000)));

        entityManager.flush();
        entityManager.clear();

        Coach updatedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals("Changed", updatedCoach.getFirstName());
        assertEquals("Coach", updatedCoach.getLastName());
        assertEquals(team.getId(), updatedCoach.getTeam().getId());
        assertEquals(15, updatedCoach.getYearsOfExperience());
        assertEquals(4, updatedCoach.getChampionshipsWon());
        assertEquals(
                0,
                new BigDecimal("3500000")
                        .compareTo(updatedCoach.getSalary())
        );
    }

    @Test
    void shouldMoveCoachToAnotherTeamUsingPatch()
            throws Exception {

        authenticateAsAdmin();

        Team oldTeam = createTeam(
                "Patch Coach Old Team " + unique()
        );

        Team newTeam = createTeam(
                "Patch Coach New Team " + unique()
        );

        Coach coach = createCoach(
                "Transfer",
                "ByPatch",
                new BigDecimal("4000000"),
                12,
                2,
                oldTeam
        );

        PatchCoachRequest request = new PatchCoachRequest(
                null,
                null,
                null,
                newTeam.getId(),
                null,
                null
        );

        String response = mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",
                        is(coach.getId().intValue())))
                .andExpect(jsonPath("$.firstName",
                        is("Transfer")))
                .andExpect(jsonPath("$.teamName",
                        is(newTeam.getName())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseCoachDto result =
                objectMapper.readValue(response, ResponseCoachDto.class);

        assertEquals(newTeam.getName(), result.teamName());

        entityManager.flush();
        entityManager.clear();

        Coach updatedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(updatedCoach.getTeam());
        assertEquals(newTeam.getId(), updatedCoach.getTeam().getId());

        assertTrue(
                coachRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .noneMatch(c -> c.getId().equals(coach.getId()))
        );

        assertTrue(
                coachRepository.findAllByTeamId(newTeam.getId())
                        .stream()
                        .anyMatch(c -> c.getId().equals(coach.getId()))
        );
    }

    @Test
    void shouldReturnBadRequestWhenPatchTargetsMissingTeam()
            throws Exception {

        authenticateAsAdmin();

        Team oldTeam = createTeam(
                "Patch Missing Coach Old Team " + unique()
        );

        Coach coach = createCoach(
                "Patch",
                "MissingTeam",
                new BigDecimal("3000000"),
                10,
                1,
                oldTeam
        );

        PatchCoachRequest request = new PatchCoachRequest(
                null,
                null,
                null,
                999999L,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldKeepCoachInSameTeamWhenPatchContainsSameTeamId()
            throws Exception {

        authenticateAsAdmin();

        Team team = createTeam(
                "Same Patch Coach Team " + unique()
        );

        Coach coach = createCoach(
                "Same",
                "Team",
                new BigDecimal("2500000"),
                8,
                0,
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

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Same")))
                .andExpect(jsonPath("$.lastName", is("Team")))
                .andExpect(jsonPath("$.teamName", is(team.getName())));

        entityManager.flush();
        entityManager.clear();

        Coach updatedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(updatedCoach.getTeam());
        assertEquals(team.getId(), updatedCoach.getTeam().getId());
    }

    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToUpdateCoach()
            throws Exception {

        authenticateAsUser();

        Coach coach = createCoach(
                "Regular",
                "User",
                new BigDecimal("2000000"),
                7,
                0,
                null
        );

        PatchCoachRequest request = new PatchCoachRequest(
                "Hacked",
                "ChangedLastName",
                new BigDecimal("9000000"),
                null,
                99,
                99
        );

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

        entityManager.flush();
        entityManager.clear();

        Coach unchangedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals("Regular", unchangedCoach.getFirstName());
        assertEquals("User", unchangedCoach.getLastName());
        assertEquals(
                0,
                new BigDecimal("2000000")
                        .compareTo(unchangedCoach.getSalary())
        );
        assertEquals(7, unchangedCoach.getYearsOfExperience());
        assertEquals(0, unchangedCoach.getChampionshipsWon());
        assertNull(unchangedCoach.getTeam());
    }

    @Test
    void shouldReturnUnauthorizedWhenUnauthenticatedUserTriesToUpdateCoach()
            throws Exception {

        Coach coach = createCoach(
                "Anonymous",
                "Coach",
                new BigDecimal("2000000"),
                7,
                0,
                null
        );

        PatchCoachRequest request = new PatchCoachRequest(
                "Hacked",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingCoach()
            throws Exception {

        authenticateAsAdmin();

        PatchCoachRequest request = new PatchCoachRequest(
                "Updated",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", 999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenPatchContainsInvalidData()
            throws Exception {

        authenticateAsAdmin();

        Coach coach = createCoach(
                "Patch",
                "Validation",
                new BigDecimal("3000000"),
                10,
                0,
                null
        );

        PatchCoachRequest request = new PatchCoachRequest(
                null,
                null,
                BigDecimal.ZERO,
                null,
                -1,
                -1
        );

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenPatchCoachRequestIsMalformedJson()
            throws Exception {

        authenticateAsAdmin();

        Coach coach = createCoach(
                "Malformed",
                "Patch",
                new BigDecimal("2500000"),
                8,
                0,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "firstName": "Updated",
                                            "salary":
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowPatchWithNoFields() throws Exception {
        authenticateAsAdmin();

        Coach coach = createCoach(
                "NoOp",
                "Coach",
                new BigDecimal("3500000"),
                13,
                2,
                null
        );

        PatchCoachRequest request = new PatchCoachRequest(
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("NoOp")))
                .andExpect(jsonPath("$.lastName", is("Coach")))
                .andExpect(jsonPath("$.yearsExperience", is(13)))
                .andExpect(jsonPath("$.championshipWon", is(2)))
                .andExpect(jsonPath("$.salary", is(3500000)));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackPatchWhenTargetTeamIsFull()
            throws Exception {

        authenticateAsAdmin();

        Team oldTeam = createTeam(
                "Patch Rollback Coach Old " + unique()
        );

        Team fullTeam = createTeam(
                "Patch Rollback Coach Full " + unique()
        );

        Coach coach = createCoach(
                "Rollback",
                "Coach",
                new BigDecimal("3000000"),
                10,
                1,
                oldTeam
        );

        for (int i = 1; i <= 5; i++) {
            createCoach(
                    "Full" + i,
                    "Coach",
                    new BigDecimal("2000000"),
                    8,
                    0,
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

        mockMvc.perform(
                        patch("/api/admin/coaches/{id}", coach.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("The coaching staff is full. Maximum 5 coaches allowed.")));

        entityManager.clear();

        Coach unchangedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertEquals("Rollback", unchangedCoach.getFirstName());
        assertNotNull(unchangedCoach.getTeam());
        assertEquals(oldTeam.getId(), unchangedCoach.getTeam().getId());

        assertTrue(
                coachRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .anyMatch(c -> c.getId().equals(coach.getId()))
        );

        assertEquals(
                5,
                coachRepository.findAllByTeamId(fullTeam.getId()).size()
        );
    }

    // =========================================================
    // DELETE COACH
    // =========================================================

    @Test
    void shouldDeleteCoachAsAdmin() throws Exception {
        authenticateAsAdmin();

        Coach coach = createCoach(
                "Delete",
                "Coach",
                new BigDecimal("2500000"),
                8,
                1,
                null
        );

        mockMvc.perform(
                        delete("/api/admin/coaches/{id}", coach.getId())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.message",
                                containsString(
                                        "Coach with id " + coach.getId()
                                                + " was deleted successfully"
                                )
                        )
                );

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository.findById(coach.getId()).isEmpty()
        );
    }

    @Test
    void shouldDeleteCoachFromTeamAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam(
                "Delete Coach From Team " + unique()
        );

        Coach coach = createCoach(
                "Delete",
                "FromTeam",
                new BigDecimal("2500000"),
                8,
                1,
                team
        );

        mockMvc.perform(
                        delete("/api/admin/coaches/{id}", coach.getId())
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository.findById(coach.getId()).isEmpty()
        );

        assertTrue(
                coachRepository.findAllByTeamId(team.getId())
                        .stream()
                        .noneMatch(c -> c.getId().equals(coach.getId()))
        );
    }

    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToDeleteCoach()
            throws Exception {

        authenticateAsUser();

        Coach coach = createCoach(
                "Delete",
                "Forbidden",
                new BigDecimal("2500000"),
                8,
                1,
                null
        );

        mockMvc.perform(
                        delete("/api/admin/coaches/{id}", coach.getId())
                )
                .andExpect(status().isForbidden());

        entityManager.flush();
        entityManager.clear();

        assertTrue(
                coachRepository.findById(coach.getId()).isPresent()
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenUnauthenticatedUserTriesToDeleteCoach()
            throws Exception {

        Coach coach = createCoach(
                "Delete",
                "Anonymous",
                new BigDecimal("2500000"),
                8,
                1,
                null
        );

        mockMvc.perform(
                        delete("/api/admin/coaches/{id}", coach.getId())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingCoach()
            throws Exception {

        authenticateAsAdmin();

        mockMvc.perform(
                        delete("/api/admin/coaches/{id}", 999999L)
                )
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // CHANGE COACH TEAM
    // =========================================================

    @Test
    void shouldChangeCoachTeamAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team oldTeam = createTeam(
                "Coach Trade Old Team " + unique()
        );

        Team newTeam = createTeam(
                "Coach Trade New Team " + unique()
        );

        Coach coach = createCoach(
                "Trade",
                "Coach",
                new BigDecimal("4000000"),
                15,
                3,
                oldTeam
        );

        String response = mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                                .param(
                                        "newTeamId",
                                        newTeam.getId().toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.memberId",
                                is(coach.getId().intValue())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.memberFullName",
                                is("Trade Coach")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.memberRole",
                                is("COACH")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.oldTeamName",
                                is(oldTeam.getName())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.newTeamId",
                                is(newTeam.getId().intValue())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.newTeamName",
                                is(newTeam.getName())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.message",
                                containsString(
                                        "Successfully traded COACH Trade Coach"
                                )
                        )
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode transferResponse =
                objectMapper.readTree(response);

        assertEquals(
                newTeam.getName(),
                transferResponse
                        .get("newTeamName")
                        .asText()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updatedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(updatedCoach.getTeam());
        assertEquals(
                newTeam.getId(),
                updatedCoach.getTeam().getId()
        );

        assertTrue(
                coachRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .noneMatch(
                                c -> c.getId()
                                        .equals(coach.getId())
                        )
        );

        assertTrue(
                coachRepository.findAllByTeamId(newTeam.getId())
                        .stream()
                        .anyMatch(
                                c -> c.getId()
                                        .equals(coach.getId())
                        )
        );
    }

    @Test
    void shouldMoveFreeAgentToTeamAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team newTeam = createTeam(
                "Free Agent Coach Target Team " + unique()
        );

        Coach coach = createCoach(
                "Free",
                "ToTeam",
                new BigDecimal("3000000"),
                10,
                1,
                null
        );

        String response = mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                                .param(
                                        "newTeamId",
                                        newTeam.getId().toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.memberId",
                                is(coach.getId().intValue())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.memberFullName",
                                is("Free ToTeam")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.memberRole",
                                is("COACH")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.oldTeamName",
                                is("Free Agent")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.newTeamId",
                                is(newTeam.getId().intValue())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.newTeamName",
                                is(newTeam.getName())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.message",
                                containsString(
                                        "Successfully traded COACH Free ToTeam to"
                                )
                        )
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode result =
                objectMapper.readTree(response);

        assertEquals(
                newTeam.getName(),
                result.get("newTeamName").asText()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updatedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(updatedCoach.getTeam());
        assertEquals(
                newTeam.getId(),
                updatedCoach.getTeam().getId()
        );
    }

    @Test
    void shouldMakeCoachFreeAgentAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam(
                "Free Agent Coach Team " + unique()
        );

        Coach coach = createCoach(
                "Free",
                "Agent",
                new BigDecimal("3000000"),
                10,
                1,
                team
        );

        String response = mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.memberId",
                                is(coach.getId().intValue())
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.memberFullName",
                                is("Free Agent")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.memberRole",
                                is("COACH")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.oldTeamName",
                                is(team.getName())
                        )
                )
                .andExpect(
                        jsonPath("$.newTeamId")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath(
                                "$.newTeamName",
                                is("Free Agent")
                        )
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode transferResponse =
                objectMapper.readTree(response);

        assertEquals(
                "Free Agent",
                transferResponse
                        .get("newTeamName")
                        .asText()
        );

        entityManager.flush();
        entityManager.clear();

        Coach updatedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNull(updatedCoach.getTeam());

        assertTrue(
                coachRepository.findAllByTeamId(team.getId())
                        .stream()
                        .noneMatch(
                                c -> c.getId()
                                        .equals(coach.getId())
                        )
        );
    }

    @Test
    void shouldReturnBadRequestWhenMakingAlreadyFreeAgentCoachFreeAgent()
            throws Exception {

        authenticateAsAdmin();

        Coach coach = createCoach(
                "Already",
                "FreeAgent",
                new BigDecimal("2500000"),
                8,
                0,
                null
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenChangingCoachToSameTeam()
            throws Exception {

        authenticateAsAdmin();

        Team team = createTeam(
                "Same Coach Team " + unique()
        );

        Coach coach = createCoach(
                "Same",
                "Team",
                new BigDecimal("2500000"),
                8,
                0,
                team
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                                .param(
                                        "newTeamId",
                                        team.getId().toString()
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldReturnBadRequestWhenChangingCoachToFullTeam()
            throws Exception {

        authenticateAsAdmin();

        Team oldTeam = createTeam(
                "Trade Coach Old Team " + unique()
        );

        Team fullTeam = createTeam(
                "Trade Coach Full Team " + unique()
        );

        Coach coach = createCoach(
                "Blocked",
                "Trade",
                new BigDecimal("3000000"),
                10,
                1,
                oldTeam
        );

        for (int i = 1; i <= 5; i++) {
            createCoach(
                    "Existing" + i,
                    "Full",
                    new BigDecimal("2000000"),
                    8,
                    0,
                    fullTeam
            );
        }

        mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                                .param(
                                        "newTeamId",
                                        fullTeam.getId().toString()
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is(
                                        "The coaching staff is full. Maximum 5 coaches allowed."
                                )
                        )
                );

        entityManager.clear();

        Coach unchangedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(unchangedCoach.getTeam());
        assertEquals(
                oldTeam.getId(),
                unchangedCoach.getTeam().getId()
        );

        assertTrue(
                coachRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .anyMatch(
                                c -> c.getId()
                                        .equals(coach.getId())
                        )
        );

        assertEquals(
                5,
                coachRepository
                        .findAllByTeamId(fullTeam.getId())
                        .size()
        );
    }

    @Test
    void shouldReturnBadRequestWhenChangingCoachToMissingTeam()
            throws Exception {

        authenticateAsAdmin();

        Coach coach = createCoach(
                "Missing",
                "Team",
                new BigDecimal("2500000"),
                8,
                0,
                null
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                                .param(
                                        "newTeamId",
                                        "999999"
                                )
                )
                .andExpect(status().isBadRequest());

        entityManager.flush();
        entityManager.clear();

        Coach unchangedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNull(unchangedCoach.getTeam());
    }

    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToChangeCoachTeam()
            throws Exception {

        authenticateAsUser();

        Team oldTeam = createTeam(
                "Forbidden Coach Old Team " + unique()
        );

        Team newTeam = createTeam(
                "Forbidden Coach New Team " + unique()
        );

        Coach coach = createCoach(
                "Change",
                "Forbidden",
                new BigDecimal("2500000"),
                8,
                0,
                oldTeam
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                                .param(
                                        "newTeamId",
                                        newTeam.getId().toString()
                                )
                )
                .andExpect(status().isForbidden());

        entityManager.flush();
        entityManager.clear();

        Coach unchangedCoach = coachRepository
                .findWithTeamById(coach.getId())
                .orElseThrow();

        assertNotNull(unchangedCoach.getTeam());
        assertEquals(
                oldTeam.getId(),
                unchangedCoach.getTeam().getId()
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenUnauthenticatedUserTriesToChangeCoachTeam()
            throws Exception {

        Team oldTeam = createTeam(
                "Anonymous Coach Old Team " + unique()
        );

        Team newTeam = createTeam(
                "Anonymous Coach New Team " + unique()
        );

        Coach coach = createCoach(
                "Anonymous",
                "Trade",
                new BigDecimal("2500000"),
                8,
                0,
                oldTeam
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                coach.getId()
                        )
                                .param(
                                        "newTeamId",
                                        newTeam.getId().toString()
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenChangingMissingCoachTeam()
            throws Exception {

        authenticateAsAdmin();

        Team newTeam = createTeam(
                "Missing Coach Target Team " + unique()
        );

        mockMvc.perform(
                        patch(
                                "/api/admin/coaches/{coachId}/change-team",
                                999999L
                        )
                                .param(
                                        "newTeamId",
                                        newTeam.getId().toString()
                                )
                )
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void authenticateAsAdmin() {
        authenticate(
                "admin_" + unique(),
                UserRole.ROLE_ADMIN
        );
    }

    private void authenticateAsUser() {
        authenticate(
                "user_" + unique(),
                UserRole.ROLE_USER
        );
    }

    private void authenticate(
            String username,
            UserRole role
    ) {
        User user = User.builder()
                .username(username)
                .password(
                        passwordEncoder.encode("password")
                )
                .email(username + "@example.com")
                .roles(Set.of(role))
                .registeredAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        savedUser.getUsername(),
                        null,
                        savedUser.getRoles()
                                .stream()
                                .map(userRole ->
                                        new SimpleGrantedAuthority(
                                                userRole.name()
                                        )
                                )
                                .toList()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

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
            BigDecimal salary,
            Integer yearsOfExperience,
            Integer championshipsWon,
            Team team
    ) {
        Coach coach = Coach.builder()
                .firstName(firstName)
                .lastName(lastName)
                .salary(salary)
                .yearsOfExperience(yearsOfExperience)
                .championshipsWon(championshipsWon)
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

