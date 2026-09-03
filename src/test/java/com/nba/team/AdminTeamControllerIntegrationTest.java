package com.nba.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.coach.Coach;
import com.nba.coach.CoachRepository;
import com.nba.core.dto.response.MessageResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.player.Player;
import com.nba.player.PlayerPosition;
import com.nba.player.PlayerRepository;
import com.nba.security.CustomUserDetails;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminTeamControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // ============================================================
    // POST /api/admin/teams
    // ============================================================

    @Test
    void shouldCreateTeam() throws Exception {

        RequestTeamDto request = new RequestTeamDto(
                "Phoenix Suns Integration Test",
                3,
                1968
        );

        Authentication auth = createAdminAuthentication(
                "admin_create_team"
        );

        String responseContent = mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.id())
                .isNotNull();

        assertThat(response.name())
                .isEqualTo("Phoenix Suns Integration Test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(3);

        assertThat(response.creationYear())
                .isEqualTo(1968);

        assertThat(response.players())
                .isEmpty();

        assertThat(response.coaches())
                .isEmpty();

        entityManager.flush();
        entityManager.clear();

        assertThat(teamRepository.findById(response.id()))
                .isPresent();
    }


    @Test
    void shouldPersistCreatedTeamInDatabase() throws Exception {

        RequestTeamDto request = new RequestTeamDto(
                "admin_persisted_team_test",
                5,
                1980
        );

        Authentication auth = createAdminAuthentication(
                "admin_persisted_team"
        );

        String responseContent = mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        entityManager.flush();
        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(response.id())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_persisted_team_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(5);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1980);
    }


    @Test
    void shouldCreateTeamWithZeroChampionshipsAndCreationYear()
            throws Exception {

        RequestTeamDto request = new RequestTeamDto(
                "Oklahoma City Thunder Integration Test",
                0,
                0
        );

        Authentication auth = createAdminAuthentication(
                "admin_create_zero_team"
        );

        String responseContent = mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.name())
                .isEqualTo("Oklahoma City Thunder Integration Test");

        assertThat(response.championshipTitleCount())
                .isZero();

        assertThat(response.creationYear())
                .isZero();
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithDuplicateName()
            throws Exception {

        createAndSaveTeam(
                "admin_duplicate_team_test",
                7,
                1946
        );

        RequestTeamDto request = new RequestTeamDto(
                "admin_duplicate_team_test",
                8,
                1946
        );

        Authentication auth = createAdminAuthentication(
                "admin_duplicate_team"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(containsString("already")));
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithBlankName()
            throws Exception {

        String invalidRequest = """
                {
                  "name": "   ",
                  "championshipCount": 3,
                  "creationYear": 1968
                }
                """;

        Authentication auth = createAdminAuthentication(
                "admin_blank_team_name"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithNullName()
            throws Exception {

        String invalidRequest = """
                {
                  "name": null,
                  "championshipCount": 3,
                  "creationYear": 1968
                }
                """;

        Authentication auth = createAdminAuthentication(
                "admin_null_team_name"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithNullChampionshipCount()
            throws Exception {

        String invalidRequest = """
                {
                  "name": "admin_null_championship_team_test",
                  "championshipCount": null,
                  "creationYear": 1968
                }
                """;

        Authentication auth = createAdminAuthentication(
                "admin_null_championship_count"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithNullCreationYear()
            throws Exception {

        String invalidRequest = """
                {
                  "name": "admin_null_creation_year_team_test",
                  "championshipCount": 3,
                  "creationYear": null
                }
                """;

        Authentication auth = createAdminAuthentication(
                "admin_null_creation_year"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithNegativeValues()
            throws Exception {

        String invalidRequest = """
                {
                  "name": "admin_negative_values_team_test",
                  "championshipCount": -1,
                  "creationYear": -1
                }
                """;

        Authentication auth = createAdminAuthentication(
                "admin_negative_team_values"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithMissingRequiredFields()
            throws Exception {

        String invalidRequest = """
                {
                  "name": "admin_missing_fields_team_test"
                }
                """;

        Authentication auth = createAdminAuthentication(
                "admin_missing_team_fields"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenCreatingTeamWithMalformedJson()
            throws Exception {

        Authentication auth = createAdminAuthentication(
                "admin_malformed_create_team"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "admin_malformed_json_team_test",
                                          "championshipCount": 3,
                                          "creationYear":
                                        }
                                        """)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenCreatingTeamWithoutAuthentication()
            throws Exception {

        RequestTeamDto request = new RequestTeamDto(
                "admin_unauthorized_create_team_test",
                3,
                1968
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenCreatingTeamAsRegularUser()
            throws Exception {

        RequestTeamDto request = new RequestTeamDto(
                "admin_forbidden_create_team_test",
                3,
                1968
        );

        Authentication auth = createUserAuthentication(
                "regular_create_team_user"
        );

        mockMvc.perform(
                        post("/api/admin/teams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // DELETE /api/admin/teams/{id}
    // ============================================================

    @Test
    void shouldDeleteTeam() throws Exception {

        Team team = createAndSaveTeam(
                "admin_delete_lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "LeBron",
                "James",
                team
        );

        Coach coach = createAndSaveCoach(
                "Darvin",
                "Ham",
                team
        );

        Authentication auth = createAdminAuthentication(
                "admin_delete_team"
        );

        String responseContent = mockMvc.perform(
                        delete("/api/admin/teams/{id}", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        MessageResponse response =
                objectMapper.readValue(
                        responseContent,
                        MessageResponse.class
                );

        assertThat(response.message())
                .isEqualTo(
                        "Team with id " + team.getId()
                                + " was deleted successfully"
                );

        entityManager.flush();
        entityManager.clear();

        assertThat(teamRepository.findById(team.getId()))
                .isEmpty();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNull();
    }


    @Test
    void shouldDeleteTeamWithoutMembers()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_delete_empty_team_test",
                0,
                2020
        );

        Authentication auth = createAdminAuthentication(
                "admin_delete_empty_team"
        );

        mockMvc.perform(
                        delete("/api/admin/teams/{id}", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        assertThat(teamRepository.findById(team.getId()))
                .isEmpty();
    }


    @Test
    void shouldReturnNotFoundWhenDeletingNonExistingTeam()
            throws Exception {

        Authentication auth = createAdminAuthentication(
                "admin_delete_missing_team"
        );

        mockMvc.perform(
                        delete("/api/admin/teams/{id}", 999999L)
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenDeletingTeamWithInvalidId()
            throws Exception {

        Authentication auth = createAdminAuthentication(
                "admin_delete_invalid_team_id"
        );

        mockMvc.perform(
                        delete("/api/admin/teams/{id}", "invalid")
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenDeletingTeamWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        delete("/api/admin/teams/{id}", 1L)
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenDeletingTeamAsRegularUser()
            throws Exception {

        Authentication auth = createUserAuthentication(
                "regular_delete_team_user"
        );

        mockMvc.perform(
                        delete("/api/admin/teams/{id}", 1L)
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // POST /api/admin/teams/{teamId}/players/{playerId}
    // ============================================================

    @Test
    void shouldAddFreeAgentPlayerToTeam() throws Exception {

        Team team = createAndSaveTeam(
                "admin_add_player_lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_player"
        );

        String responseContent = mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamTransferResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamTransferResponse.class
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());

        assertThat(response.memberFullName())
                .isEqualTo("Anthony Davis");

        assertThat(response.memberRole())
                .isEqualTo("PLAYER");

        assertThat(response.oldTeamName())
                .isEqualTo("Free Agent");

        assertThat(response.newTeamName())
                .isEqualTo("admin_add_player_lakers_test");

        assertThat(response.newTeamId())
                .isEqualTo(team.getId());

        assertThat(response.message())
                .contains("Successfully added PLAYER Anthony Davis");

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());
    }


    @Test
    void shouldTradePlayerFromOneTeamToAnother()
            throws Exception {

        Team oldTeam = createAndSaveTeam(
                "admin_trade_lakers_test",
                17,
                1947
        );

        Team newTeam = createAndSaveTeam(
                "admin_trade_bulls_test",
                6,
                1966
        );

        Player player = createAndSavePlayer(
                "LeBron",
                "James",
                oldTeam
        );

        Authentication auth = createAdminAuthentication(
                "admin_trade_player"
        );

        String responseContent = mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                newTeam.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamTransferResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamTransferResponse.class
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());

        assertThat(response.memberFullName())
                .isEqualTo("LeBron James");

        assertThat(response.memberRole())
                .isEqualTo("PLAYER");

        assertThat(response.oldTeamName())
                .isEqualTo("admin_trade_lakers_test");

        assertThat(response.newTeamName())
                .isEqualTo("admin_trade_bulls_test");

        assertThat(response.newTeamId())
                .isEqualTo(newTeam.getId());

        assertThat(response.message())
                .contains(
                        "Successfully traded PLAYER LeBron James from "
                                + "admin_trade_lakers_test to admin_trade_bulls_test"
                );

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(newTeam.getId());

        Team actualOldTeam =
                teamRepository.findById(oldTeam.getId())
                        .orElseThrow();

        Team actualNewTeam =
                teamRepository.findById(newTeam.getId())
                        .orElseThrow();

        assertThat(actualOldTeam.getTeamMembers())
                .noneMatch(member ->
                        member.getId().equals(player.getId())
                );

        assertThat(actualNewTeam.getTeamMembers())
                .anyMatch(member ->
                        member.getId().equals(player.getId())
                );
    }


    @Test
    void shouldReturnBadRequestWhenAddingPlayerToTheSameTeam()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_same_player_team_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                team
        );

        Authentication auth = createAdminAuthentication(
                "admin_same_player_team"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(containsString("already exists")));

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());
    }


    @Test
    void shouldReturnBadRequestWhenPlayerDoesNotExist()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_missing_player_team_test",
                17,
                1947
        );

        Authentication auth = createAdminAuthentication(
                "admin_missing_player"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                999999L
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnNotFoundWhenAddingPlayerToNonExistingTeam()
            throws Exception {

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_player_missing_team"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                999999L,
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();
    }


    @Test
    void shouldReturnBadRequestWhenAddingPlayerWithInvalidTeamId()
            throws Exception {

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_player_invalid_team_id"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                "invalid",
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenAddingPlayerWithInvalidPlayerId()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_add_player_invalid_id_team_test",
                17,
                1947
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_player_invalid_player_id"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                "invalid"
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenAddingPlayerToFullTeam()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_full_player_roster_team_test",
                17,
                1947
        );

        for (int i = 1; i <= 15; i++) {
            createAndSavePlayer(
                    "Player",
                    "Roster" + i,
                    team
            );
        }

        Player freeAgent = createAndSavePlayer(
                "Stephen",
                "Curry",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_full_player_roster"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                freeAgent.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The team roster is full. Maximum 15 players allowed."
                        )
                );

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(freeAgent.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();
    }


    @Test
    void shouldRollbackPlayerTransferWhenNewTeamIsFull()
            throws Exception {

        Team oldTeam = createAndSaveTeam(
                "admin_rollback_player_old_team_test",
                5,
                2000
        );

        Team newTeam = createAndSaveTeam(
                "admin_rollback_player_full_team_test",
                10,
                2001
        );

        Player player = createAndSavePlayer(
                "Luka",
                "Doncic",
                oldTeam
        );

        for (int i = 1; i <= 15; i++) {
            createAndSavePlayer(
                    "Player",
                    "Full" + i,
                    newTeam
            );
        }

        Authentication auth = createAdminAuthentication(
                "admin_rollback_player_transfer"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                newTeam.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(oldTeam.getId());
    }


    @Test
    void shouldReturnUnauthorizedWhenAddingPlayerWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                1L,
                                1L
                        )
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenAddingPlayerAsRegularUser()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_forbidden_add_player_team_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "Stephen",
                "Curry",
                null
        );

        Authentication auth = createUserAuthentication(
                "regular_add_player_user"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // POST /api/admin/teams/{teamId}/coaches/{coachId}
    // ============================================================

    @Test
    void shouldAddFreeAgentCoachToTeam() throws Exception {

        Team team = createAndSaveTeam(
                "admin_add_coach_warriors_test",
                7,
                1946
        );

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_coach"
        );

        String responseContent = mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamTransferResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamTransferResponse.class
                );

        assertThat(response.memberId())
                .isEqualTo(coach.getId());

        assertThat(response.memberFullName())
                .isEqualTo("Steve Kerr");

        assertThat(response.memberRole())
                .isEqualTo("COACH");

        assertThat(response.oldTeamName())
                .isEqualTo("Free Agent");

        assertThat(response.newTeamName())
                .isEqualTo("admin_add_coach_warriors_test");

        assertThat(response.newTeamId())
                .isEqualTo(team.getId());

        assertThat(response.message())
                .contains("Successfully added COACH Steve Kerr");

        entityManager.flush();
        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNotNull();

        assertThat(actualCoach.getTeam().getId())
                .isEqualTo(team.getId());
    }


    @Test
    void shouldTradeCoachFromOneTeamToAnother()
            throws Exception {

        Team oldTeam = createAndSaveTeam(
                "admin_trade_warriors_test",
                7,
                1946
        );

        Team newTeam = createAndSaveTeam(
                "admin_trade_celtics_test",
                18,
                1946
        );

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                oldTeam
        );

        Authentication auth = createAdminAuthentication(
                "admin_trade_coach"
        );

        String responseContent = mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                newTeam.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamTransferResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamTransferResponse.class
                );

        assertThat(response.memberId())
                .isEqualTo(coach.getId());

        assertThat(response.memberFullName())
                .isEqualTo("Steve Kerr");

        assertThat(response.memberRole())
                .isEqualTo("COACH");

        assertThat(response.oldTeamName())
                .isEqualTo("admin_trade_warriors_test");

        assertThat(response.newTeamName())
                .isEqualTo("admin_trade_celtics_test");

        assertThat(response.newTeamId())
                .isEqualTo(newTeam.getId());

        assertThat(response.message())
                .contains(
                        "Successfully traded COACH Steve Kerr from "
                                + "admin_trade_warriors_test to admin_trade_celtics_test"
                );

        entityManager.flush();
        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNotNull();

        assertThat(actualCoach.getTeam().getId())
                .isEqualTo(newTeam.getId());

        Team actualOldTeam =
                teamRepository.findById(oldTeam.getId())
                        .orElseThrow();

        Team actualNewTeam =
                teamRepository.findById(newTeam.getId())
                        .orElseThrow();

        assertThat(actualOldTeam.getTeamMembers())
                .noneMatch(member ->
                        member.getId().equals(coach.getId())
                );

        assertThat(actualNewTeam.getTeamMembers())
                .anyMatch(member ->
                        member.getId().equals(coach.getId())
                );
    }


    @Test
    void shouldReturnBadRequestWhenAddingCoachToTheSameTeam()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_same_coach_team_test",
                7,
                1946
        );

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                team
        );

        Authentication auth = createAdminAuthentication(
                "admin_same_coach_team"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(containsString("already exists")));

        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNotNull();

        assertThat(actualCoach.getTeam().getId())
                .isEqualTo(team.getId());
    }


    @Test
    void shouldReturnBadRequestWhenCoachDoesNotExist()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_missing_coach_team_test",
                7,
                1946
        );

        Authentication auth = createAdminAuthentication(
                "admin_missing_coach"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                999999L
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnNotFoundWhenAddingCoachToNonExistingTeam()
            throws Exception {

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_coach_missing_team"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                999999L,
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());

        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNull();
    }


    @Test
    void shouldReturnBadRequestWhenAddingCoachWithInvalidTeamId()
            throws Exception {

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_coach_invalid_team_id"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                "invalid",
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenAddingCoachWithInvalidCoachId()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_add_coach_invalid_id_team_test",
                7,
                1946
        );

        Authentication auth = createAdminAuthentication(
                "admin_add_coach_invalid_coach_id"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                "invalid"
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenAddingCoachToFullStaff()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_full_coaching_staff_team_test",
                7,
                1946
        );

        for (int i = 1; i <= 5; i++) {
            createAndSaveCoach(
                    "Coach",
                    "Staff" + i,
                    team
            );
        }

        Coach freeAgent = createAndSaveCoach(
                "Mike",
                "Brown",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_full_coaching_staff"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                freeAgent.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(
                                "The coaching staff is full. Maximum 5 coaches allowed."
                        )
                );

        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(freeAgent.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNull();
    }


    @Test
    void shouldRollbackCoachTransferWhenNewTeamIsFull()
            throws Exception {

        Team oldTeam = createAndSaveTeam(
                "admin_rollback_coach_old_team_test",
                5,
                2002
        );

        Team newTeam = createAndSaveTeam(
                "admin_rollback_coach_full_team_test",
                10,
                2003
        );

        Coach coach = createAndSaveCoach(
                "Erik",
                "Spoelstra",
                oldTeam
        );

        for (int i = 1; i <= 5; i++) {
            createAndSaveCoach(
                    "Coach",
                    "Full" + i,
                    newTeam
            );
        }

        Authentication auth = createAdminAuthentication(
                "admin_rollback_coach_transfer"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                newTeam.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());

        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNotNull();

        assertThat(actualCoach.getTeam().getId())
                .isEqualTo(oldTeam.getId());
    }


    @Test
    void shouldReturnUnauthorizedWhenAddingCoachWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                1L,
                                1L
                        )
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenAddingCoachAsRegularUser()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_forbidden_add_coach_team_test",
                7,
                1946
        );

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                null
        );

        Authentication auth = createUserAuthentication(
                "regular_add_coach_user"
        );

        mockMvc.perform(
                        post(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // DELETE /api/admin/teams/{teamId}/players/{playerId}
    // ============================================================

    @Test
    void shouldRemovePlayerFromTeam() throws Exception {

        Team team = createAndSaveTeam(
                "admin_remove_player_lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                team
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_player"
        );

        String responseContent = mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamTransferResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamTransferResponse.class
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());

        assertThat(response.memberFullName())
                .isEqualTo("Anthony Davis");

        assertThat(response.memberRole())
                .isEqualTo("PLAYER");

        assertThat(response.oldTeamName())
                .isEqualTo("admin_remove_player_lakers_test");

        assertThat(response.newTeamName())
                .isEqualTo("Free Agent");

        assertThat(response.newTeamId())
                .isNull();

        assertThat(response.message())
                .contains(
                        "Successfully removed PLAYER Anthony Davis"
                );

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getTeamMembers())
                .noneMatch(member ->
                        member.getId().equals(player.getId())
                );
    }


    @Test
    void shouldReturnBadRequestWhenRemovingPlayerThatIsNotInTeam()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_remove_missing_player_team_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_missing_player_from_team"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();
    }


    @Test
    void shouldReturnBadRequestWhenRemovingPlayerFromAnotherTeam()
            throws Exception {

        Team targetTeam = createAndSaveTeam(
                "admin_remove_wrong_player_target_team_test",
                17,
                1947
        );

        Team actualTeam = createAndSaveTeam(
                "admin_remove_wrong_player_actual_team_test",
                6,
                1966
        );

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                actualTeam
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_wrong_player_team"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                targetTeam.getId(),
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(actualTeam.getId());
    }


    @Test
    void shouldReturnNotFoundWhenRemovingPlayerFromNonExistingTeam()
            throws Exception {

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_player_missing_team"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                999999L,
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenRemovingPlayerWithInvalidTeamId()
            throws Exception {

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_player_invalid_team_id"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                "invalid",
                                player.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenRemovingPlayerWithInvalidPlayerId()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_remove_player_invalid_id_team_test",
                17,
                1947
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_player_invalid_player_id"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                team.getId(),
                                "invalid"
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenRemovingPlayerWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                1L,
                                1L
                        )
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenRemovingPlayerAsRegularUser()
            throws Exception {

        Authentication auth = createUserAuthentication(
                "regular_remove_player_user"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/players/{playerId}",
                                1L,
                                1L
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // DELETE /api/admin/teams/{teamId}/coaches/{coachId}
    // ============================================================

    @Test
    void shouldRemoveCoachFromTeam() throws Exception {

        Team team = createAndSaveTeam(
                "admin_remove_coach_warriors_test",
                7,
                1946
        );

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                team
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_coach"
        );

        String responseContent = mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamTransferResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamTransferResponse.class
                );

        assertThat(response.memberId())
                .isEqualTo(coach.getId());

        assertThat(response.memberFullName())
                .isEqualTo("Steve Kerr");

        assertThat(response.memberRole())
                .isEqualTo("COACH");

        assertThat(response.oldTeamName())
                .isEqualTo("admin_remove_coach_warriors_test");

        assertThat(response.newTeamName())
                .isEqualTo("Free Agent");

        assertThat(response.newTeamId())
                .isNull();

        assertThat(response.message())
                .contains(
                        "Successfully removed COACH Steve Kerr"
                );

        entityManager.flush();
        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNull();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getTeamMembers())
                .noneMatch(member ->
                        member.getId().equals(coach.getId())
                );
    }


    @Test
    void shouldReturnBadRequestWhenRemovingCoachThatIsNotInTeam()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_remove_missing_coach_team_test",
                5,
                2020
        );

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_missing_coach_from_team"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());

        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNull();
    }


    @Test
    void shouldReturnBadRequestWhenRemovingCoachFromAnotherTeam()
            throws Exception {

        Team targetTeam = createAndSaveTeam(
                "admin_remove_wrong_coach_target_team_test",
                7,
                1946
        );

        Team actualTeam = createAndSaveTeam(
                "admin_remove_wrong_coach_actual_team_test",
                18,
                1946
        );

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                actualTeam
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_wrong_coach_team"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                targetTeam.getId(),
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());

        entityManager.clear();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNotNull();

        assertThat(actualCoach.getTeam().getId())
                .isEqualTo(actualTeam.getId());
    }


    @Test
    void shouldReturnNotFoundWhenRemovingCoachFromNonExistingTeam()
            throws Exception {

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_coach_missing_team"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                999999L,
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenRemovingCoachWithInvalidTeamId()
            throws Exception {

        Coach coach = createAndSaveCoach(
                "Steve",
                "Kerr",
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_coach_invalid_team_id"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                "invalid",
                                coach.getId()
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenRemovingCoachWithInvalidCoachId()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_remove_coach_invalid_id_team_test",
                7,
                1946
        );

        Authentication auth = createAdminAuthentication(
                "admin_remove_coach_invalid_coach_id"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                team.getId(),
                                "invalid"
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenRemovingCoachWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                1L,
                                1L
                        )
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenRemovingCoachAsRegularUser()
            throws Exception {

        Authentication auth = createUserAuthentication(
                "regular_remove_coach_user"
        );

        mockMvc.perform(
                        delete(
                                "/api/admin/teams/{teamId}/coaches/{coachId}",
                                1L,
                                1L
                        )
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // DELETE /api/admin/teams/{id}/members
    // ============================================================

    @Test
    void shouldFireAllTeamMembers() throws Exception {

        Team team = createAndSaveTeam(
                "admin_fire_all_members_lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "LeBron",
                "James",
                team
        );

        Player secondPlayer = createAndSavePlayer(
                "Anthony",
                "Davis",
                team
        );

        Coach coach = createAndSaveCoach(
                "Darvin",
                "Ham",
                team
        );

        Authentication auth = createAdminAuthentication(
                "admin_fire_all_members"
        );

        String responseContent = mockMvc.perform(
                        delete("/api/admin/teams/{id}/members", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        MessageResponse response =
                objectMapper.readValue(
                        responseContent,
                        MessageResponse.class
                );

        assertThat(response.message())
                .isEqualTo(
                        "all team members were fired from team with id "
                                + team.getId()
                );

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();

        Player actualSecondPlayer =
                playerRepository.findById(secondPlayer.getId())
                        .orElseThrow();

        assertThat(actualSecondPlayer.getTeam())
                .isNull();

        Coach actualCoach =
                coachRepository.findById(coach.getId())
                        .orElseThrow();

        assertThat(actualCoach.getTeam())
                .isNull();

        assertThat(teamRepository.findById(team.getId()))
                .isPresent();
    }


    @Test
    void shouldFireAllMembersWhenTeamIsAlreadyEmpty()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_fire_empty_team_test",
                0,
                2020
        );

        Authentication auth = createAdminAuthentication(
                "admin_fire_empty_team"
        );

        String responseContent = mockMvc.perform(
                        delete("/api/admin/teams/{id}/members", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        MessageResponse response =
                objectMapper.readValue(
                        responseContent,
                        MessageResponse.class
                );

        assertThat(response.message())
                .isEqualTo(
                        "all team members were fired from team with id "
                                + team.getId()
                );

        entityManager.flush();
        entityManager.clear();

        assertThat(teamRepository.findById(team.getId()))
                .isPresent();
    }


    @Test
    void shouldReturnNotFoundWhenFiringMembersFromNonExistingTeam()
            throws Exception {

        Authentication auth = createAdminAuthentication(
                "admin_fire_missing_team"
        );

        mockMvc.perform(
                        delete("/api/admin/teams/{id}/members", 999999L)
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenFiringMembersWithInvalidId()
            throws Exception {

        Authentication auth = createAdminAuthentication(
                "admin_fire_invalid_team_id"
        );

        mockMvc.perform(
                        delete("/api/admin/teams/{id}/members", "invalid")
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenFiringMembersWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        delete("/api/admin/teams/{id}/members", 1L)
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenFiringMembersAsRegularUser()
            throws Exception {

        Authentication auth = createUserAuthentication(
                "regular_fire_members_user"
        );

        mockMvc.perform(
                        delete("/api/admin/teams/{id}/members", 1L)
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // PATCH /api/admin/teams/{id}
    // ============================================================

    @Test
    void shouldUpdateAllTeamFields() throws Exception {

        Team team = createAndSaveTeam(
                "admin_update_lakers_test",
                17,
                1947
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_update_lakers_updated_test",
                18,
                1948
        );

        Authentication auth = createAdminAuthentication(
                "admin_update_team"
        );

        String responseContent = mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.id())
                .isEqualTo(team.getId());

        assertThat(response.name())
                .isEqualTo("admin_update_lakers_updated_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(18);

        assertThat(response.creationYear())
                .isEqualTo(1948);

        entityManager.flush();
        entityManager.clear();

        Team updatedTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(updatedTeam.getName())
                .isEqualTo("admin_update_lakers_updated_test");

        assertThat(updatedTeam.getChampionshipTitleCount())
                .isEqualTo(18);

        assertThat(updatedTeam.getCreationYear())
                .isEqualTo(1948);
    }


    @Test
    void shouldUpdateOnlyTeamName() throws Exception {

        Team team = createAndSaveTeam(
                "admin_update_name_only_team_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_update_name_only_target_test",
                null,
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_update_name_only"
        );

        String responseContent = mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.name())
                .isEqualTo("admin_update_name_only_target_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(6);

        assertThat(response.creationYear())
                .isEqualTo(1966);

        entityManager.flush();
        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_update_name_only_target_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(6);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1966);
    }


    @Test
    void shouldUpdateOnlyChampionshipCount() throws Exception {

        Team team = createAndSaveTeam(
                "admin_update_championship_team_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                null,
                10,
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_update_championship_only"
        );

        String responseContent = mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.name())
                .isEqualTo("admin_update_championship_team_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(10);

        assertThat(response.creationYear())
                .isEqualTo(1966);

        entityManager.flush();
        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_update_championship_team_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(10);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1966);
    }


    @Test
    void shouldUpdateOnlyCreationYear() throws Exception {

        Team team = createAndSaveTeam(
                "admin_update_year_team_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                null,
                null,
                1990
        );

        Authentication auth = createAdminAuthentication(
                "admin_update_year_only"
        );

        String responseContent = mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.name())
                .isEqualTo("admin_update_year_team_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(6);

        assertThat(response.creationYear())
                .isEqualTo(1990);

        entityManager.flush();
        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_update_year_team_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(6);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1990);
    }


    @Test
    void shouldIgnoreNullFieldsDuringPatch() throws Exception {

        Team team = createAndSaveTeam(
                "admin_patch_null_fields_test",
                8,
                1990
        );

        Authentication auth = createAdminAuthentication(
                "admin_patch_null_fields"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "teamName": null,
                                          "championshipCount": null,
                                          "creationYear": null
                                        }
                                        """)
                                .with(authentication(auth))
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_patch_null_fields_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(8);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1990);
    }


    @Test
    void shouldAllowPatchWithoutProvidedFields()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_patch_empty_request_team_test",
                6,
                1966
        );

        String requestJson = "{}";

        Authentication auth = createAdminAuthentication(
                "admin_patch_empty_request"
        );

        String responseContent = mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.name())
                .isEqualTo("admin_patch_empty_request_team_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(6);

        assertThat(response.creationYear())
                .isEqualTo(1966);
    }


    @Test
    void shouldAllowUpdatingTeamWithSameName()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_same_name_update_team_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_same_name_update_team_test",
                null,
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_same_name_update"
        );

        String responseContent = mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.name())
                .isEqualTo("admin_same_name_update_team_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(6);

        assertThat(response.creationYear())
                .isEqualTo(1966);
    }


    @Test
    void shouldAllowUpdatingTeamToZeroValues()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_zero_update_team_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                null,
                0,
                0
        );

        Authentication auth = createAdminAuthentication(
                "admin_zero_update"
        );

        String responseContent = mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ResponseTeamDto response =
                objectMapper.readValue(
                        responseContent,
                        ResponseTeamDto.class
                );

        assertThat(response.name())
                .isEqualTo("admin_zero_update_team_test");

        assertThat(response.championshipTitleCount())
                .isZero();

        assertThat(response.creationYear())
                .isZero();
    }


    @Test
    void shouldReturnBadRequestWhenUpdatingTeamWithDuplicateName()
            throws Exception {

        createAndSaveTeam(
                "admin_update_existing_team_test",
                17,
                1947
        );

        Team team = createAndSaveTeam(
                "admin_update_target_team_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_update_existing_team_test",
                null,
                null
        );

        Authentication auth = createAdminAuthentication(
                "admin_duplicate_team_update"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(containsString("already")));

        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_update_target_team_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(6);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1966);
    }


    @Test
    void shouldReturnBadRequestWhenUpdatingDuplicateNameWithOtherFields()
            throws Exception {

        createAndSaveTeam(
                "admin_duplicate_combined_existing_team_test",
                17,
                1947
        );

        Team team = createAndSaveTeam(
                "admin_duplicate_combined_target_team_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_duplicate_combined_existing_team_test",
                20,
                2005
        );

        Authentication auth = createAdminAuthentication(
                "admin_duplicate_combined_update"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(containsString("already")));

        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_duplicate_combined_target_team_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(6);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1966);
    }


    @Test
    void shouldReturnBadRequestWhenUpdatingTeamWithInvalidData()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_invalid_update_team_test",
                6,
                1966
        );

        String invalidRequest = """
                {
                  "teamName": "admin_invalid_update_team_test",
                  "championshipCount": -1,
                  "creationYear": -10
                }
                """;

        Authentication auth = createAdminAuthentication(
                "admin_invalid_team_update"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenUpdatingTeamWithMalformedJson()
            throws Exception {

        Team team = createAndSaveTeam(
                "admin_malformed_patch_team_test",
                6,
                1966
        );

        Authentication auth = createAdminAuthentication(
                "admin_malformed_patch"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", team.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "teamName": "admin_malformed_patch_target_test",
                                          "championshipCount":
                                        }
                                        """)
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());

        entityManager.clear();

        Team actualTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(actualTeam.getName())
                .isEqualTo("admin_malformed_patch_team_test");

        assertThat(actualTeam.getChampionshipTitleCount())
                .isEqualTo(6);

        assertThat(actualTeam.getCreationYear())
                .isEqualTo(1966);
    }


    @Test
    void shouldReturnBadRequestWhenUpdatingTeamWithNullBody()
            throws Exception {

        Authentication auth = createAdminAuthentication(
                "admin_null_update_body"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("")
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingTeam()
            throws Exception {

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_missing_update_team_test",
                3,
                1988
        );

        Authentication auth = createAdminAuthentication(
                "admin_update_missing_team"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", 999999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenUpdatingTeamWithInvalidId()
            throws Exception {

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_invalid_id_update_team_test",
                3,
                1988
        );

        Authentication auth = createAdminAuthentication(
                "admin_update_invalid_team_id"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", "invalid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenUpdatingTeamWithoutAuthentication()
            throws Exception {

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_unauthorized_update_team_test",
                3,
                1988
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnForbiddenWhenUpdatingTeamAsRegularUser()
            throws Exception {

        PatchTeamRequest request = new PatchTeamRequest(
                "admin_forbidden_update_team_test",
                3,
                1988
        );

        Authentication auth = createUserAuthentication(
                "regular_update_team_user"
        );

        mockMvc.perform(
                        patch("/api/admin/teams/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(authentication(auth))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // Helpers
    // ============================================================

    private Authentication createAdminAuthentication(String username) {
        return createAuthentication(
                username,
                UserRole.ROLE_ADMIN
        );
    }


    private Authentication createUserAuthentication(String username) {
        return createAuthentication(
                username,
                UserRole.ROLE_USER
        );
    }


    private Authentication createAuthentication(
            String username,
            UserRole role
    ) {

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .email(username + "@test.com")
                .roles(Set.of(role))
                .registeredAt(LocalDateTime.now())
                .build();

        User savedUser =
                userRepository.save(user);

        CustomUserDetails userDetails =
                new CustomUserDetails(savedUser);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }


    private Team createAndSaveTeam(
            String name,
            Integer championshipCount,
            Integer creationYear
    ) {

        Team team = Team.builder()
                .name(name)
                .championshipTitleCount(championshipCount)
                .creationYear(creationYear)
                .teamMembers(new ArrayList<>())
                .build();

        return teamRepository.save(team);
    }


    private Player createAndSavePlayer(
            String firstName,
            String lastName,
            Team team
    ) {

        Player player = Player.builder()
                .firstName(firstName)
                .lastName(lastName)
                .salary(BigDecimal.valueOf(35000000))
                .team(team)
                .playerPositions(Set.of(PlayerPosition.PG))
                .rating(90)
                .championshipsWon(2)
                .build();

        Player savedPlayer =
                playerRepository.save(player);

        if (team != null) {
            team.addTeamMember(savedPlayer);
        }

        return savedPlayer;
    }


    private Coach createAndSaveCoach(
            String firstName,
            String lastName,
            Team team
    ) {

        Coach coach = Coach.builder()
                .firstName(firstName)
                .lastName(lastName)
                .salary(BigDecimal.valueOf(12000000))
                .team(team)
                .yearsOfExperience(12)
                .championshipsWon(3)
                .build();

        Coach savedCoach =
                coachRepository.save(coach);

        if (team != null) {
            team.addTeamMember(savedCoach);
        }

        return savedCoach;
    }
}

