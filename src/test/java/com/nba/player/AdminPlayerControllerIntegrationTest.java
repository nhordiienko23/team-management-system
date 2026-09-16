package com.nba.player;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
class AdminPlayerControllerIntegrationTest extends AbstractIntegrationTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlayerRepository playerRepository;

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
// CREATE PLAYER
// =========================================================

    @Test
    void shouldCreatePlayerAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Create Team " + unique());

        RequestPlayerDto request = new RequestPlayerDto(
                "John",
                "Create",
                new BigDecimal("1500000"),
                team.getId(),
                Set.of(PlayerPosition.SG),
                85,
                2
        );

        String response = mockMvc.perform(
                        post("/api/admin/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Create")))
                .andExpect(jsonPath("$.team", is(team.getName())))
                .andExpect(jsonPath("$.rating", is(85)))
                .andExpect(jsonPath("$.championshipWon", is(2)))
                .andExpect(jsonPath("$.salary", is(1500000)))
                .andExpect(jsonPath("$.positions", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponsePlayerDto result =
                objectMapper.readValue(response, ResponsePlayerDto.class);

        assertNotNull(result.id());

        entityManager.flush();
        entityManager.clear();

        Player savedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(result.id())
                .orElseThrow();

        assertEquals("John", savedPlayer.getFirstName());
        assertEquals("Create", savedPlayer.getLastName());
        assertEquals(team.getId(), savedPlayer.getTeam().getId());
        assertEquals(85, savedPlayer.getRating());
        assertEquals(Set.of(PlayerPosition.SG), savedPlayer.getPlayerPositions());
    }


    @Test
    void shouldCreatePlayerAsFreeAgent() throws Exception {
        authenticateAsAdmin();

        RequestPlayerDto request = new RequestPlayerDto(
                "Free",
                "Agent",
                new BigDecimal("1200000"),
                null,
                Set.of(PlayerPosition.PG),
                78,
                1
        );

        String response = mockMvc.perform(
                        post("/api/admin/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("Free")))
                .andExpect(jsonPath("$.lastName", is("Agent")))
                .andExpect(jsonPath("$.team").doesNotExist())
                .andExpect(jsonPath("$.rating", is(78)))
                .andExpect(jsonPath("$.positions", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponsePlayerDto result =
                objectMapper.readValue(response, ResponsePlayerDto.class);

        entityManager.flush();
        entityManager.clear();

        Player savedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(result.id())
                .orElseThrow();

        assertNull(savedPlayer.getTeam());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingPlayerForMissingTeam() throws Exception {
        authenticateAsAdmin();

        RequestPlayerDto request = new RequestPlayerDto(
                "Missing",
                "Team",
                new BigDecimal("1200000"),
                999999L,
                Set.of(PlayerPosition.PG),
                78,
                1
        );

        mockMvc.perform(
                        post("/api/admin/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingPlayerInFullTeam() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Full Team " + unique());

        for (int i = 1; i <= 15; i++) {
            createPlayer(
                    "Existing" + i,
                    "Player",
                    new BigDecimal("1000000"),
                    Set.of(PlayerPosition.SG),
                    70,
                    0,
                    team
            );
        }

        RequestPlayerDto request = new RequestPlayerDto(
                "Sixteenth",
                "Player",
                new BigDecimal("1500000"),
                team.getId(),
                Set.of(PlayerPosition.SG),
                80,
                0
        );

        mockMvc.perform(
                        post("/api/admin/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("The team roster is full. Maximum 15 players allowed.")));
    }


    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToCreatePlayer() throws Exception {
        authenticateAsUser();

        RequestPlayerDto request = new RequestPlayerDto(
                "John",
                "User",
                new BigDecimal("1000000"),
                null,
                Set.of(PlayerPosition.PG),
                80,
                0
        );

        mockMvc.perform(
                        post("/api/admin/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void shouldReturnUnauthorizedWhenUnauthenticatedUserTriesToCreatePlayer() throws Exception {
        RequestPlayerDto request = new RequestPlayerDto(
                "John",
                "Anonymous",
                new BigDecimal("1000000"),
                null,
                Set.of(PlayerPosition.PG),
                80,
                0
        );

        mockMvc.perform(
                        post("/api/admin/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingPlayerWithInvalidData() throws Exception {
        authenticateAsAdmin();

        RequestPlayerDto request = new RequestPlayerDto(
                "",
                "",
                BigDecimal.ZERO,
                null,
                Set.of(),
                101,
                -1
        );

        mockMvc.perform(
                        post("/api/admin/players")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatePlayerRequestIsMalformedJson() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(
                        post("/api/admin/players")
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
// PATCH PLAYER
// =========================================================

    @Test
    void shouldPartiallyUpdatePlayerAsAdmin() throws Exception {
        authenticateAsAdmin();

        Player player = createPlayer(
                "Old",
                "Player",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.SG),
                70,
                1,
                null
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                "Updated",
                "Player",
                new BigDecimal("2500000"),
                null,
                Set.of(PlayerPosition.PG, PlayerPosition.SG),
                92,
                5
        );

        String response = mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.lastName", is("Player")))
                .andExpect(jsonPath("$.rating", is(92)))
                .andExpect(jsonPath("$.championshipWon", is(5)))
                .andExpect(jsonPath("$.salary", is(2500000)))
                .andExpect(jsonPath("$.positions", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponsePlayerDto result =
                objectMapper.readValue(response, ResponsePlayerDto.class);

        assertEquals(player.getId(), result.id());

        entityManager.flush();
        entityManager.clear();

        Player updatedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertEquals("Updated", updatedPlayer.getFirstName());
        assertEquals(
                0,
                new BigDecimal("2500000").compareTo(updatedPlayer.getSalary())
        );
        assertEquals(92, updatedPlayer.getRating());
        assertEquals(5, updatedPlayer.getChampionshipsWon());
        assertEquals(
                Set.of(PlayerPosition.PG, PlayerPosition.SG),
                updatedPlayer.getPlayerPositions()
        );
    }


    @Test
    void shouldPartiallyUpdateOnlyProvidedFields() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Partial Update Team " + unique());

        Player player = createPlayer(
                "Original",
                "Player",
                new BigDecimal("3500000"),
                Set.of(PlayerPosition.SF),
                88,
                4,
                team
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                "Changed",
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Changed")))
                .andExpect(jsonPath("$.lastName", is("Player")))
                .andExpect(jsonPath("$.team", is(team.getName())))
                .andExpect(jsonPath("$.rating", is(88)))
                .andExpect(jsonPath("$.championshipWon", is(4)))
                .andExpect(jsonPath("$.salary", is(3500000)))
                .andExpect(jsonPath("$.positions", hasSize(1)));

        entityManager.flush();
        entityManager.clear();

        Player updatedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertEquals("Changed", updatedPlayer.getFirstName());
        assertEquals("Player", updatedPlayer.getLastName());
        assertEquals(team.getId(), updatedPlayer.getTeam().getId());
        assertEquals(88, updatedPlayer.getRating());
        assertEquals(4, updatedPlayer.getChampionshipsWon());
        assertEquals(
                0,
                new BigDecimal("3500000").compareTo(updatedPlayer.getSalary())
        );
        assertEquals(Set.of(PlayerPosition.SF), updatedPlayer.getPlayerPositions());
    }


    @Test
    void shouldMovePlayerToAnotherTeamUsingPatch() throws Exception {
        authenticateAsAdmin();

        Team oldTeam = createTeam("Patch Old Team " + unique());
        Team newTeam = createTeam("Patch New Team " + unique());

        Player player = createPlayer(
                "Transfer",
                "ByPatch",
                new BigDecimal("2000000"),
                Set.of(PlayerPosition.PF),
                85,
                2,
                oldTeam
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                null,
                null,
                null,
                newTeam.getId(),
                null,
                null,
                null
        );

        String response = mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(player.getId().intValue())))
                .andExpect(jsonPath("$.firstName", is("Transfer")))
                .andExpect(jsonPath("$.team", is(newTeam.getName())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponsePlayerDto result =
                objectMapper.readValue(response, ResponsePlayerDto.class);

        assertEquals(newTeam.getName(), result.team());

        entityManager.flush();
        entityManager.clear();

        Player updatedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNotNull(updatedPlayer.getTeam());
        assertEquals(newTeam.getId(), updatedPlayer.getTeam().getId());

        assertTrue(
                playerRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .noneMatch(p -> p.getId().equals(player.getId()))
        );

        assertTrue(
                playerRepository.findAllByTeamId(newTeam.getId())
                        .stream()
                        .anyMatch(p -> p.getId().equals(player.getId()))
        );
    }


    @Test
    void shouldReturnBadRequestWhenPatchTargetsMissingTeam() throws Exception {
        authenticateAsAdmin();

        Team oldTeam = createTeam("Patch Missing Old Team " + unique());

        Player player = createPlayer(
                "Patch",
                "MissingTeam",
                new BigDecimal("1500000"),
                Set.of(PlayerPosition.PG),
                80,
                0,
                oldTeam
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                null,
                null,
                null,
                999999L,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldKeepPlayerInSameTeamWhenPatchContainsSameTeamId() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Same Patch Team " + unique());

        Player player = createPlayer(
                "Same",
                "Team",
                new BigDecimal("1500000"),
                Set.of(PlayerPosition.SG),
                80,
                0,
                team
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                null,
                null,
                null,
                team.getId(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Same")))
                .andExpect(jsonPath("$.lastName", is("Team")))
                .andExpect(jsonPath("$.team", is(team.getName())));

        entityManager.flush();
        entityManager.clear();

        Player updatedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNotNull(updatedPlayer.getTeam());
        assertEquals(team.getId(), updatedPlayer.getTeam().getId());
    }


    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToUpdatePlayer() throws Exception {
        authenticateAsUser();

        Player player = createPlayer(
                "Regular",
                "User",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.PG),
                70,
                0,
                null
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                "Hacked",
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void shouldReturnNotFoundWhenUpdatingMissingPlayer() throws Exception {
        authenticateAsAdmin();

        PatchPlayerRequest request = new PatchPlayerRequest(
                "Updated",
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", 999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenPatchContainsInvalidData() throws Exception {
        authenticateAsAdmin();

        Player player = createPlayer(
                "Patch",
                "Validation",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.PG),
                80,
                0,
                null
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                null,
                null,
                BigDecimal.ZERO,
                null,
                null,
                101,
                -1
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldAllowPatchWithNoFields() throws Exception {
        authenticateAsAdmin();

        Player player = createPlayer(
                "NoOp",
                "Player",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.C),
                75,
                2,
                null
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("NoOp")))
                .andExpect(jsonPath("$.lastName", is("Player")))
                .andExpect(jsonPath("$.rating", is(75)))
                .andExpect(jsonPath("$.championshipWon", is(2)));
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackPatchWhenTargetTeamIsFull() throws Exception {
        authenticateAsAdmin();

        Team oldTeam = createTeam("Patch Rollback Old " + unique());
        Team fullTeam = createTeam("Patch Rollback Full " + unique());

        Player player = createPlayer(
                "Rollback",
                "Player",
                new BigDecimal("2000000"),
                Set.of(PlayerPosition.SG),
                85,
                1,
                oldTeam
        );

        for (int i = 1; i <= 15; i++) {
            createPlayer(
                    "Full" + i,
                    "Player",
                    new BigDecimal("1000000"),
                    Set.of(PlayerPosition.PF),
                    70,
                    0,
                    fullTeam
            );
        }

        PatchPlayerRequest request = new PatchPlayerRequest(
                "ShouldRollback",
                null,
                null,
                fullTeam.getId(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{id}", player.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("The team roster is full. Maximum 15 players allowed.")));

        entityManager.clear();

        Player unchangedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertEquals("Rollback", unchangedPlayer.getFirstName());
        assertNotNull(unchangedPlayer.getTeam());
        assertEquals(oldTeam.getId(), unchangedPlayer.getTeam().getId());

        assertTrue(
                playerRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .anyMatch(p -> p.getId().equals(player.getId()))
        );

        assertEquals(
                15,
                playerRepository.findAllByTeamId(fullTeam.getId()).size()
        );
    }


// =========================================================
// DELETE PLAYER
// =========================================================

    @Test
    void shouldDeletePlayerAsAdmin() throws Exception {
        authenticateAsAdmin();

        Player player = createPlayer(
                "Delete",
                "Player",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.C),
                75,
                1,
                null
        );

        mockMvc.perform(
                        delete("/api/admin/players/{id}", player.getId())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.message",
                                containsString(
                                        "Player with id " + player.getId()
                                                + " was deleted successfully"
                                )
                        )
                );

        entityManager.flush();
        entityManager.clear();

        assertTrue(playerRepository.findById(player.getId()).isEmpty());
    }


    @Test
    void shouldDeletePlayerFromTeamAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Delete From Team " + unique());

        Player player = createPlayer(
                "Delete",
                "FromTeam",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.SF),
                75,
                1,
                team
        );

        mockMvc.perform(
                        delete("/api/admin/players/{id}", player.getId())
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        assertTrue(playerRepository.findById(player.getId()).isEmpty());

        assertTrue(
                playerRepository.findAllByTeamId(team.getId())
                        .stream()
                        .noneMatch(p -> p.getId().equals(player.getId()))
        );
    }


    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToDeletePlayer() throws Exception {
        authenticateAsUser();

        Player player = createPlayer(
                "Delete",
                "Forbidden",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.SF),
                75,
                1,
                null
        );

        mockMvc.perform(
                        delete("/api/admin/players/{id}", player.getId())
                )
                .andExpect(status().isForbidden());

        entityManager.flush();
        entityManager.clear();

        assertTrue(playerRepository.findById(player.getId()).isPresent());
    }


    @Test
    void shouldReturnUnauthorizedWhenUnauthenticatedUserTriesToDeletePlayer() throws Exception {
        Player player = createPlayer(
                "Delete",
                "Anonymous",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.SF),
                75,
                1,
                null
        );

        mockMvc.perform(
                        delete("/api/admin/players/{id}", player.getId())
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnNotFoundWhenDeletingMissingPlayer() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(
                        delete("/api/admin/players/{id}", 999999L)
                )
                .andExpect(status().isNotFound());
    }


// =========================================================
// CHANGE PLAYER TEAM
// =========================================================

    @Test
    void shouldChangePlayerTeamAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team oldTeam = createTeam("Old Team " + unique());
        Team newTeam = createTeam("New Team " + unique());

        Player player = createPlayer(
                "Trade",
                "Player",
                new BigDecimal("3000000"),
                Set.of(PlayerPosition.SF),
                90,
                3,
                oldTeam
        );

        String response = mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                                .param("newTeamId", newTeam.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId", is(player.getId().intValue())))
                .andExpect(jsonPath("$.memberFullName", is("Trade Player")))
                .andExpect(jsonPath("$.memberRole", is("PLAYER")))
                .andExpect(jsonPath("$.oldTeamName", is(oldTeam.getName())))
                .andExpect(jsonPath("$.newTeamId", is(newTeam.getId().intValue())))
                .andExpect(jsonPath("$.newTeamName", is(newTeam.getName())))
                .andExpect(
                        jsonPath(
                                "$.message",
                                containsString("Successfully traded PLAYER Trade Player")
                        )
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode transferResponse = objectMapper.readTree(response);

        assertEquals(
                newTeam.getName(),
                transferResponse.get("newTeamName").asText()
        );

        entityManager.flush();
        entityManager.clear();

        Player updatedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNotNull(updatedPlayer.getTeam());
        assertEquals(newTeam.getId(), updatedPlayer.getTeam().getId());

        assertTrue(
                playerRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .noneMatch(p -> p.getId().equals(player.getId()))
        );

        assertTrue(
                playerRepository.findAllByTeamId(newTeam.getId())
                        .stream()
                        .anyMatch(p -> p.getId().equals(player.getId()))
        );
    }


    @Test
    void shouldMoveFreeAgentToTeamAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team newTeam = createTeam("Free Agent Target Team " + unique());

        Player player = createPlayer(
                "Free",
                "ToTeam",
                new BigDecimal("2000000"),
                Set.of(PlayerPosition.PG),
                82,
                1,
                null
        );

        String response = mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                                .param("newTeamId", newTeam.getId().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId", is(player.getId().intValue())))
                .andExpect(jsonPath("$.memberFullName", is("Free ToTeam")))
                .andExpect(jsonPath("$.memberRole", is("PLAYER")))
                .andExpect(jsonPath("$.oldTeamName", is("Free Agent")))
                .andExpect(jsonPath("$.newTeamId", is(newTeam.getId().intValue())))
                .andExpect(jsonPath("$.newTeamName", is(newTeam.getName())))
                .andExpect(
                        jsonPath(
                                "$.message",
                                containsString("Successfully traded PLAYER Free ToTeam to")
                        )
                )
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode result = objectMapper.readTree(response);

        assertEquals(
                newTeam.getName(),
                result.get("newTeamName").asText()
        );

        entityManager.flush();
        entityManager.clear();

        Player updatedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNotNull(updatedPlayer.getTeam());
        assertEquals(newTeam.getId(), updatedPlayer.getTeam().getId());
    }


    @Test
    void shouldMakePlayerFreeAgentAsAdmin() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Free Agent Team " + unique());

        Player player = createPlayer(
                "Free",
                "Agent",
                new BigDecimal("2000000"),
                Set.of(PlayerPosition.PG),
                82,
                1,
                team
        );

        String response = mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId", is(player.getId().intValue())))
                .andExpect(jsonPath("$.memberFullName", is("Free Agent")))
                .andExpect(jsonPath("$.memberRole", is("PLAYER")))
                .andExpect(jsonPath("$.oldTeamName", is(team.getName())))
                .andExpect(jsonPath("$.newTeamId").doesNotExist())
                .andExpect(jsonPath("$.newTeamName", is("Free Agent")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode transferResponse = objectMapper.readTree(response);

        assertEquals(
                "Free Agent",
                transferResponse.get("newTeamName").asText()
        );

        entityManager.flush();
        entityManager.clear();

        Player updatedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNull(updatedPlayer.getTeam());

        assertTrue(
                playerRepository.findAllByTeamId(team.getId())
                        .stream()
                        .noneMatch(p -> p.getId().equals(player.getId()))
        );
    }


    @Test
    void shouldReturnBadRequestWhenChangingPlayerToSameTeam() throws Exception {
        authenticateAsAdmin();

        Team team = createTeam("Same Team " + unique());

        Player player = createPlayer(
                "Same",
                "Team",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.SG),
                80,
                0,
                team
        );

        mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                                .param("newTeamId", team.getId().toString())
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldReturnBadRequestWhenChangingPlayerToFullTeam() throws Exception {
        authenticateAsAdmin();

        Team oldTeam = createTeam("Trade Old Team " + unique());
        Team fullTeam = createTeam("Trade Full Team " + unique());

        Player player = createPlayer(
                "Blocked",
                "Trade",
                new BigDecimal("2000000"),
                Set.of(PlayerPosition.SG),
                85,
                1,
                oldTeam
        );

        for (int i = 1; i <= 15; i++) {
            createPlayer(
                    "Existing" + i,
                    "Full",
                    new BigDecimal("1000000"),
                    Set.of(PlayerPosition.PF),
                    70,
                    0,
                    fullTeam
            );
        }

        mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                                .param("newTeamId", fullTeam.getId().toString())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("The team roster is full. Maximum 15 players allowed.")));

        entityManager.clear();

        Player unchangedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNotNull(unchangedPlayer.getTeam());
        assertEquals(oldTeam.getId(), unchangedPlayer.getTeam().getId());

        assertTrue(
                playerRepository.findAllByTeamId(oldTeam.getId())
                        .stream()
                        .anyMatch(p -> p.getId().equals(player.getId()))
        );

        assertEquals(
                15,
                playerRepository.findAllByTeamId(fullTeam.getId()).size()
        );
    }


    @Test
    void shouldReturnBadRequestWhenChangingPlayerToMissingTeam() throws Exception {
        authenticateAsAdmin();

        Player player = createPlayer(
                "Missing",
                "Team",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.PG),
                80,
                0,
                null
        );

        mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                                .param("newTeamId", "999999")
                )
                .andExpect(status().isBadRequest());

        entityManager.flush();
        entityManager.clear();

        Player unchangedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNull(unchangedPlayer.getTeam());
    }


    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToChangePlayerTeam() throws Exception {
        authenticateAsUser();

        Team oldTeam = createTeam("Forbidden Old Team " + unique());
        Team newTeam = createTeam("Forbidden New Team " + unique());

        Player player = createPlayer(
                "Change",
                "Forbidden",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.C),
                80,
                0,
                oldTeam
        );

        mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                                .param("newTeamId", newTeam.getId().toString())
                )
                .andExpect(status().isForbidden());

        entityManager.flush();
        entityManager.clear();

        Player unchangedPlayer = playerRepository
                .findPlayerWithTeamAndPositionsById(player.getId())
                .orElseThrow();

        assertNotNull(unchangedPlayer.getTeam());
        assertEquals(oldTeam.getId(), unchangedPlayer.getTeam().getId());
    }


    @Test
    void shouldReturnUnauthorizedWhenUnauthenticatedUserTriesToChangePlayerTeam() throws Exception {
        Team oldTeam = createTeam("Anonymous Old Team " + unique());
        Team newTeam = createTeam("Anonymous New Team " + unique());

        Player player = createPlayer(
                "Anonymous",
                "Trade",
                new BigDecimal("1000000"),
                Set.of(PlayerPosition.SG),
                80,
                0,
                oldTeam
        );

        mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", player.getId())
                                .param("newTeamId", newTeam.getId().toString())
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnNotFoundWhenChangingMissingPlayerTeam() throws Exception {
        authenticateAsAdmin();

        Team newTeam = createTeam("Missing Player Target Team " + unique());

        mockMvc.perform(
                        patch("/api/admin/players/{playerId}/change-team", 999999L)
                                .param("newTeamId", newTeam.getId().toString())
                )
                .andExpect(status().isNotFound());
    }


// =========================================================
// HELPERS
// =========================================================

    private void authenticateAsAdmin() {
        authenticate("admin_" + unique(), UserRole.ROLE_ADMIN);
    }


    private void authenticateAsUser() {
        authenticate("user_" + unique(), UserRole.ROLE_USER);
    }


    private void authenticate(String username, UserRole role) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .email(username + "@example.com")
                .roles(Set.of(role))
                .registeredAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        savedUser.getUsername(),
                        null,
                        savedUser.getRoles().stream()
                                .map(userRole ->
                                        new SimpleGrantedAuthority(userRole.name()))
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


    private Player createPlayer(
            String firstName,
            String lastName,
            BigDecimal salary,
            Set<PlayerPosition> positions,
            Integer rating,
            Integer championshipsWon,
            Team team
    ) {
        Player player = Player.builder()
                .firstName(firstName)
                .lastName(lastName)
                .salary(salary)
                .playerPositions(positions)
                .rating(rating)
                .championshipsWon(championshipsWon)
                .build();

        if (team != null) {
            team.addTeamMember(player);
        }

        return playerRepository.save(player);
    }


    private String unique() {
        return UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }


}
