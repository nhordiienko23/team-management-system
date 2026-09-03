package com.nba.team;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.coach.Coach;
import com.nba.coach.CoachRepository;
import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.player.Player;
import com.nba.player.PlayerPosition;
import com.nba.player.PlayerRepository;
import com.nba.security.CustomUserDetails;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TeamControllerIntegrationTest extends AbstractIntegrationTest {

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
    // GET /api/teams
    // ============================================================

    @Test
    void shouldReturnAllTeams() throws Exception {

        Team team1 = createAndSaveTeam(
                "lakers_controller_test",
                17,
                1947
        );

        Team team2 = createAndSaveTeam(
                "bulls_controller_test",
                6,
                1966
        );

        Authentication auth = createAuthentication(
                "team_controller_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams")
                                .param("page", "0")
                                .param("size", "100")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content")).isNotNull();
        assertThat(response.get("content").isArray()).isTrue();

        assertThat(response.get("content").toString())
                .contains(team1.getName(), team2.getName());

        assertThat(response.get("size").asInt())
                .isEqualTo(100);

        assertThat(response.get("number").asInt())
                .isZero();
    }


    @Test
    void shouldReturnSecondPageOfTeams() throws Exception {

        createAndSaveTeam(
                "controller_page_team_1",
                17,
                1947
        );

        createAndSaveTeam(
                "controller_page_team_2",
                15,
                1950
        );

        createAndSaveTeam(
                "controller_page_team_3",
                12,
                1960
        );

        Authentication auth = createAuthentication(
                "team_controller_page_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams")
                                .param("page", "1")
                                .param("size", "2")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("number").asInt())
                .isEqualTo(1);

        assertThat(response.get("size").asInt())
                .isEqualTo(2);

        assertThat(response.get("content").isArray())
                .isTrue();

        assertThat(response.get("content").size())
                .isLessThanOrEqualTo(2);
    }


    @Test
    void shouldReturnEmptyPageWhenTeamsPageIsOutOfRange() throws Exception {

        Authentication auth = createAuthentication(
                "team_controller_out_of_range_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams")
                                .param("page", "1000")
                                .param("size", "10")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content"))
                .isNotNull();

        assertThat(response.get("content").size())
                .isZero();
    }


    @Test
    void shouldReturnUnauthorizedWhenGettingAllTeamsWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/teams")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // GET /api/teams/{id}
    // ============================================================

    @Test
    void shouldReturnTeamById() throws Exception {

        Team team = createAndSaveTeam(
                "lakers_controller_test",
                17,
                1947
        );

        Authentication auth = createAuthentication(
                "team_controller_get_by_id_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/{id}", team.getId())
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
                .isEqualTo("lakers_controller_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(17);

        assertThat(response.creationYear())
                .isEqualTo(1947);

        assertThat(response.players())
                .isEmpty();

        assertThat(response.coaches())
                .isEmpty();
    }


    @Test
    void shouldReturnTeamWithPlayersAndCoaches() throws Exception {

        Team team = createAndSaveTeam(
                "lakers_controller_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "nikita_controller_test",
                "player_controller_test",
                team
        );

        Coach coach = createAndSaveCoach(
                "sveta_controller_test",
                "coach_controller_test",
                team
        );

        Authentication auth = createAuthentication(
                "team_controller_members_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/{id}", team.getId())
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

        assertThat(response.players())
                .hasSize(1);

        assertThat(response.coaches())
                .hasSize(1);

        assertThat(response.players().get(0).id())
                .isEqualTo(player.getId());

        assertThat(response.players().get(0).fullName())
                .isEqualTo(
                        "nikita_controller_test player_controller_test"
                );

        assertThat(response.coaches().get(0).id())
                .isEqualTo(coach.getId());

        assertThat(response.coaches().get(0).fullName())
                .isEqualTo(
                        "sveta_controller_test coach_controller_test"
                );
    }


    @Test
    void shouldReturnNotFoundWhenTeamDoesNotExist() throws Exception {

        Authentication auth = createAuthentication(
                "team_controller_not_found_user"
        );

        mockMvc.perform(
                        get("/api/teams/{id}", 999999L)
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenTeamIdIsInvalid() throws Exception {

        Authentication auth = createAuthentication(
                "team_controller_invalid_id_user"
        );

        mockMvc.perform(
                        get("/api/teams/{id}", "invalid")
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenGettingTeamByIdWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/teams/{id}", 1)
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // GET /api/teams/search
    // ============================================================

    @Test
    void shouldReturnTeamsWithoutSearchFilters() throws Exception {

        Team team1 = createAndSaveTeam(
                "controller_search_lakers_test",
                17,
                1947
        );

        Team team2 = createAndSaveTeam(
                "controller_search_bulls_test",
                6,
                1966
        );

        Authentication auth = createAuthentication(
                "team_controller_search_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content").toString())
                .contains(team1.getName(), team2.getName());

        assertThat(response.get("number").asInt())
                .isZero();
    }


    @Test
    void shouldSearchTeamsByName() throws Exception {

        Team target = createAndSaveTeam(
                "controller_search_unique_lakers_test",
                17,
                1947
        );

        createAndSaveTeam(
                "controller_search_unique_bulls_test",
                6,
                1966
        );

        Authentication auth = createAuthentication(
                "team_controller_search_name_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("teamName", "unique_lakers")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content").size())
                .isEqualTo(1);

        assertThat(response.get("content").get(0).get("name").asText())
                .isEqualTo(target.getName());
    }


    @Test
    void shouldSearchTeamsByMinimumChampionshipCount() throws Exception {

        Team target = createAndSaveTeam(
                "controller_championship_min_test",
                17,
                1947
        );

        createAndSaveTeam(
                "controller_championship_low_test",
                5,
                1960
        );

        Authentication auth = createAuthentication(
                "team_controller_search_min_champ_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("minChampionshipTitleCount", "10")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content").toString())
                .contains(target.getName());
    }


    @Test
    void shouldSearchTeamsByMaximumChampionshipCount() throws Exception {

        createAndSaveTeam(
                "controller_championship_high_test",
                17,
                1947
        );

        Team target = createAndSaveTeam(
                "controller_championship_max_test",
                5,
                1960
        );

        Authentication auth = createAuthentication(
                "team_controller_search_max_champ_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("maxChampionshipTitleCount", "10")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content").toString())
                .contains(target.getName());
    }


    @Test
    void shouldSearchTeamsByMinimumCreationYear() throws Exception {

        Team target = createAndSaveTeam(
                "controller_creation_min_test",
                5,
                1990
        );

        createAndSaveTeam(
                "controller_creation_old_test",
                5,
                1960
        );

        Authentication auth = createAuthentication(
                "team_controller_search_min_year_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("minCreationYear", "1980")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content").toString())
                .contains(target.getName());
    }


    @Test
    void shouldSearchTeamsByMaximumCreationYear() throws Exception {

        createAndSaveTeam(
                "controller_creation_new_test",
                5,
                1990
        );

        Team target = createAndSaveTeam(
                "controller_creation_max_test",
                5,
                1960
        );

        Authentication auth = createAuthentication(
                "team_controller_search_max_year_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("maxCreationYear", "1980")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()                .getContentAsString();;

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content").toString())
                .contains(target.getName());
    }


    @Test
    void shouldSearchTeamsUsingMultipleFilters() throws Exception {

        Team target = createAndSaveTeam(
                "controller_search_target_test",
                17,
                1950
        );

        createAndSaveTeam(
                "controller_search_other_test",
                5,
                2000
        );

        Authentication auth = createAuthentication(
                "team_controller_search_multiple_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("teamName", "target")
                                .param("minChampionshipTitleCount", "10")
                                .param("maxChampionshipTitleCount", "20")
                                .param("minCreationYear", "1940")
                                .param("maxCreationYear", "1960")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content").size())
                .isEqualTo(1);

        assertThat(response.get("content").get(0).get("name").asText())
                .isEqualTo(target.getName());
    }


    @Test
    void shouldReturnEmptyPageWhenNoTeamsMatchSearch() throws Exception {

        createAndSaveTeam(
                "controller_existing_test",
                17,
                1947
        );

        Authentication auth = createAuthentication(
                "team_controller_search_empty_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("teamName", "controller_no_such_team")
                                .param("page", "0")
                                .param("size", "100")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("content"))
                .isNotNull();

        assertThat(response.get("content").size())
                .isZero();
    }


    @Test
    void shouldReturnPaginatedSearchResults() throws Exception {

        createAndSaveTeam(
                "controller_pagination_team_1",
                17,
                1947
        );

        createAndSaveTeam(
                "controller_pagination_team_2",
                15,
                1950
        );

        createAndSaveTeam(
                "controller_pagination_team_3",
                12,
                1960
        );

        Authentication auth = createAuthentication(
                "team_controller_search_pagination_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/search")
                                .param("teamName", "controller_pagination")
                                .param("page", "1")
                                .param("size", "2")
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseContent);

        assertThat(response.get("number").asInt())
                .isEqualTo(1);

        assertThat(response.get("size").asInt())
                .isEqualTo(2);

        assertThat(response.get("totalElements").asInt())
                .isEqualTo(3);

        assertThat(response.get("content").size())
                .isEqualTo(1);
    }


    @Test
    void shouldReturnUnauthorizedWhenSearchingTeamsWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/teams/search")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // GET /api/teams/{teamId}/team-lineup
    // ============================================================

    @Test
    void shouldReturnTeamLineup() throws Exception {

        Team team = createAndSaveTeam(
                "controller_lineup_test",
                17,
                1947
        );

        Player player1 = createAndSavePlayer(
                "nikita_controller_test",
                "player1_controller_test",
                team
        );

        Player player2 = createAndSavePlayer(
                "nikita_controller_test",
                "player2_controller_test",
                team
        );

        createAndSaveCoach(
                "sveta_controller_test",
                "coach_controller_test",
                team
        );

        Authentication auth = createAuthentication(
                "team_controller_lineup_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/{teamId}/team-lineup", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamGroupResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamGroupResponse.class
                );

        assertThat(response.teamName())
                .isEqualTo("controller_lineup_test");

        assertThat(response.title())
                .isEqualTo(TeamGroupType.TEAM_LINEUP.name());

        assertThat(response.members())
                .hasSize(2);

        assertThat(response.members())
                .extracting(member -> member.fullName())
                .contains(
                        "nikita_controller_test player1_controller_test",
                        "nikita_controller_test player2_controller_test"
                );
    }


    @Test
    void shouldReturnEmptyTeamLineupWhenTeamHasNoPlayers()
            throws Exception {

        Team team = createAndSaveTeam(
                "controller_empty_lineup_test",
                17,
                1947
        );

        createAndSaveCoach(
                "sveta_controller_test",
                "coach_controller_test",
                team
        );

        Authentication auth = createAuthentication(
                "team_controller_empty_lineup_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/{teamId}/team-lineup", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamGroupResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamGroupResponse.class
                );

        assertThat(response.teamName())
                .isEqualTo("controller_empty_lineup_test");

        assertThat(response.title())
                .isEqualTo(TeamGroupType.TEAM_LINEUP.name());

        assertThat(response.members())
                .isEmpty();
    }


    @Test
    void shouldReturnNotFoundWhenGettingLineupOfNonExistingTeam()
            throws Exception {

        Authentication auth = createAuthentication(
                "team_controller_lineup_not_found_user"
        );

        mockMvc.perform(
                        get("/api/teams/{teamId}/team-lineup", 999999L)
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenLineupTeamIdIsInvalid()
            throws Exception {

        Authentication auth = createAuthentication(
                "team_controller_lineup_invalid_id_user"
        );

        mockMvc.perform(
                        get("/api/teams/{teamId}/team-lineup", "invalid")
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenGettingTeamLineupWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/teams/{teamId}/team-lineup", 1)
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // GET /api/teams/{teamId}/coaching-staff
    // ============================================================

    @Test
    void shouldReturnCoachingStaff() throws Exception {

        Team team = createAndSaveTeam(
                "controller_coaching_test",
                17,
                1947
        );

        createAndSavePlayer(
                "nikita_controller_test",
                "player_controller_test",
                team
        );

        createAndSaveCoach(
                "sveta_controller_test",
                "coach1_controller_test",
                team
        );

        createAndSaveCoach(
                "sveta_controller_test",
                "coach2_controller_test",
                team
        );

        Authentication auth = createAuthentication(
                "team_controller_coaching_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/{teamId}/coaching-staff", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamGroupResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamGroupResponse.class
                );

        assertThat(response.teamName())
                .isEqualTo("controller_coaching_test");

        assertThat(response.title())
                .isEqualTo(TeamGroupType.COACHING_STAFF.name());

        assertThat(response.members())
                .hasSize(2);

        assertThat(response.members())
                .extracting(member -> member.fullName())
                .contains(
                        "sveta_controller_test coach1_controller_test",
                        "sveta_controller_test coach2_controller_test"
                );
    }


    @Test
    void shouldReturnEmptyCoachingStaffWhenTeamHasNoCoaches()
            throws Exception {

        Team team = createAndSaveTeam(
                "controller_empty_coaching_test",
                17,
                1947
        );

        createAndSavePlayer(
                "nikita_controller_test",
                "player_controller_test",
                team
        );

        Authentication auth = createAuthentication(
                "team_controller_empty_coaching_user"
        );

        String responseContent = mockMvc.perform(
                        get("/api/teams/{teamId}/coaching-staff", team.getId())
                                .with(authentication(auth))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TeamGroupResponse response =
                objectMapper.readValue(
                        responseContent,
                        TeamGroupResponse.class
                );

        assertThat(response.teamName())
                .isEqualTo("controller_empty_coaching_test");

        assertThat(response.title())
                .isEqualTo(TeamGroupType.COACHING_STAFF.name());

        assertThat(response.members())
                .isEmpty();
    }


    @Test
    void shouldReturnNotFoundWhenGettingCoachingStaffOfNonExistingTeam()
            throws Exception {

        Authentication auth = createAuthentication(
                "team_controller_coaching_not_found_user"
        );

        mockMvc.perform(
                        get("/api/teams/{teamId}/coaching-staff", 999999L)
                                .with(authentication(auth))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenCoachingStaffTeamIdIsInvalid()
            throws Exception {

        Authentication auth = createAuthentication(
                "team_controller_coaching_invalid_id_user"
        );

        mockMvc.perform(
                        get("/api/teams/{teamId}/coaching-staff", "invalid")
                                .with(authentication(auth))
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnUnauthorizedWhenGettingCoachingStaffWithoutAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/api/teams/{teamId}/coaching-staff", 1)
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // Helpers
    // ============================================================

    private Authentication createAuthentication(String username) {

        User user = createAndSaveUser(username);

        CustomUserDetails userDetails =
                new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }


    private User createAndSaveUser(String username) {

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .email(username + "@test.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .registeredAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
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
                .salary(BigDecimal.valueOf(100000))
                .team(team)
                .playerPositions(Set.of(PlayerPosition.PG))
                .rating(80)
                .championshipsWon(2)
                .build();

        Player savedPlayer =
                playerRepository.save(player);

        team.addTeamMember(savedPlayer);

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
                .salary(BigDecimal.valueOf(150000))
                .team(team)
                .yearsOfExperience(10)
                .championshipsWon(3)
                .build();

        Coach savedCoach =
                coachRepository.save(coach);

        team.addTeamMember(savedCoach);

        return savedCoach;
    }
}