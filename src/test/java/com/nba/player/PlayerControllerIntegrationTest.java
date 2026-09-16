
package com.nba.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.security.CustomUserDetails;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlayerControllerIntegrationTest extends AbstractIntegrationTest {

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


    // ============================================================
    // GET /api/players
    // ============================================================

    @Test
    void shouldGetAllPlayersForAuthenticatedUser() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.list.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players")
                                .with(authentication(authentication))
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }


    @Test
    void shouldGetAllPlayersUsingDefaultPagination() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.default.pagination.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players")
                                .with(authentication(authentication))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }


    @Test
    void shouldReturnUnauthorizedWhenGettingAllPlayersWithoutAuthentication() throws Exception {

        SecurityContextHolder.clearContext();

        mockMvc.perform(
                        get("/api/players")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldReturnEmptyPageWhenRequestedPageIsOutOfRange() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.page.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players")
                                .with(authentication(authentication))
                                .param("page", "1000")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }


    // ============================================================
    // GET /api/players/{id}
    // ============================================================

    @Test
    void shouldGetPlayerByIdForAuthenticatedUser() throws Exception {

        Team team = createAndSaveTeam(
                "Los Angeles Clippers",
                0,
                1970
        );

        Player player = createAndSavePlayer(
                "Michael",
                "Jordan",
                team
        );

        entityManager.flush();
        entityManager.clear();

        Authentication authentication =
                createUserAuthentication(
                        "players.get.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players/{id}", player.getId())
                                .with(authentication(authentication))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(player.getId()))
                .andExpect(jsonPath("$.firstName").value("Michael"))
                .andExpect(jsonPath("$.lastName").value("Jordan"))
                .andExpect(jsonPath("$.team").value("Los Angeles Clippers"))
                .andExpect(jsonPath("$.rating").value(90))
                .andExpect(jsonPath("$.championshipWon").value(2))
                .andExpect(jsonPath("$.salary").value(35000000));
    }


    @Test
    void shouldReturnNotFoundWhenPlayerDoesNotExist() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.notfound.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players/{id}", 999999L)
                                .with(authentication(authentication))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnUnauthorizedWhenGettingPlayerWithoutAuthentication() throws Exception {

        SecurityContextHolder.clearContext();

        mockMvc.perform(
                        get("/api/players/{id}", 1L)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // GET /api/players/search
    // ============================================================

    @Test
    void shouldSearchPlayersByNameForAuthenticatedUser() throws Exception {

        Team team = createAndSaveTeam(
                "Phoenix Suns",
                0,
                1968
        );

        createAndSavePlayer(
                "ScottieTest",
                "PippenTest",
                team
        );

        entityManager.flush();
        entityManager.clear();

        Authentication authentication =
                createUserAuthentication(
                        "players.search.name.user",
                        UserRole.ROLE_USER
                );

        String responseBody =
                mockMvc.perform(
                                get("/api/players/search")
                                        .with(authentication(authentication))
                                        .param("firstName", "scottietest")
                                        .param("page", "0")
                                        .param("size", "10")
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json =
                objectMapper.readTree(responseBody);

        JsonNode playerJson =
                json.path("content")
                        .get(0);

        assertThat(playerJson.path("firstName").asText())
                .isEqualTo("ScottieTest");

        assertThat(playerJson.path("lastName").asText())
                .isEqualTo("PippenTest");

        assertThat(playerJson.path("team").asText())
                .isEqualTo("Phoenix Suns");
    }


    @Test
    void shouldSearchPlayersUsingMultipleFilters() throws Exception {

        Team team = createAndSaveTeam(
                "Sacramento Kings",
                2,
                1948
        );

        Player matchingPlayer =
                Player.builder()
                        .firstName("Hakeem")
                        .lastName("Olajuwon")
                        .salary(BigDecimal.valueOf(40000000))
                        .team(team)
                        .playerPositions(
                                new HashSet<>(
                                        Set.of(
                                                PlayerPosition.C,
                                                PlayerPosition.PF
                                        )
                                )
                        )
                        .rating(95)
                        .championshipsWon(2)
                        .build();

        Player wrongRatingPlayer =
                Player.builder()
                        .firstName("Kareem")
                        .lastName("Abdul-Jabbar")
                        .salary(BigDecimal.valueOf(40000000))
                        .team(team)
                        .playerPositions(
                                new HashSet<>(
                                        Set.of(PlayerPosition.C)
                                )
                        )
                        .rating(80)
                        .championshipsWon(2)
                        .build();

        playerRepository.save(matchingPlayer);
        playerRepository.save(wrongRatingPlayer);

        team.addTeamMember(matchingPlayer);
        team.addTeamMember(wrongRatingPlayer);

        entityManager.flush();
        entityManager.clear();

        Authentication authentication =
                createUserAuthentication(
                        "players.search.filter.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players/search")
                                .with(authentication(authentication))
                                .param("firstName", "Hakeem")
                                .param("teamName", "sacramento")
                                .param("minRating", "95")
                                .param("maxRating", "95")
                                .param("minSalary", "40000000")
                                .param("maxSalary", "40000000")
                                .param("positions", "C")
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Hakeem"))
                .andExpect(jsonPath("$.content[0].lastName").value("Olajuwon"))
                .andExpect(jsonPath("$.content[0].team").value("Sacramento Kings"))
                .andExpect(jsonPath("$.content[0].rating").value(95))
                .andExpect(jsonPath("$.content[0].salary").value(40000000))
                .andExpect(jsonPath("$.content[0].championshipWon").value(2));
    }


    @Test
    void shouldReturnAllPlayersWhenSearchHasNoFilters() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.search.default.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players/search")
                                .with(authentication(authentication))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(
                        jsonPath(
                                "$.totalElements",
                                org.hamcrest.Matchers.greaterThan(0)
                        )
                )
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.first").value(true));
    }


    @Test
    void shouldReturnEmptySearchResultWhenNoPlayersMatch() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.search.empty.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players/search")
                                .with(authentication(authentication))
                                .param("firstName", "NobodyWithThisName")
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }


    @Test
    void shouldReturnEmptyResultForUnknownPosition() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.search.position.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players/search")
                                .with(authentication(authentication))
                                .param("positions", "UNKNOWN_POSITION")
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }


    @Test
    void shouldReturnSearchResultsWithPagination() throws Exception {

        Team team = createAndSaveTeam(
                "Indiana Pacers",
                3,
                1967
        );

        for (int i = 1; i <= 5; i++) {
            createAndSavePlayer(
                    "Larry",
                    "Bird" + i,
                    team
            );
        }

        entityManager.flush();
        entityManager.clear();

        Authentication authentication =
                createUserAuthentication(
                        "players.search.pagination.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get("/api/players/search")
                                .with(authentication(authentication))
                                .param("teamName", "Indiana Pacers")
                                .param("page", "0")
                                .param("size", "2")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }


    @Test
    void shouldReturnUnauthorizedWhenSearchingWithoutAuthentication() throws Exception {

        SecurityContextHolder.clearContext();

        mockMvc.perform(
                        get("/api/players/search")
                                .param("firstName", "Michael")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // GET /api/players/{playerId}/teammates
    // ============================================================

    @Test
    void shouldGetPlayerTeammatesForAuthenticatedUser() throws Exception {

        Team team = createAndSaveTeam(
                "Detroit Pistons",
                3,
                1941
        );

        Player michaelJordan =
                createAndSavePlayer(
                        "Michael",
                        "Jordan",
                        team
                );

        createAndSavePlayer(
                "Isiah",
                "Thomas",
                team
        );

        createAndSavePlayer(
                "Dennis",
                "Rodman",
                team
        );

        entityManager.flush();
        entityManager.clear();

        Authentication authentication =
                createUserAuthentication(
                        "players.teammates.user",
                        UserRole.ROLE_USER
                );

        String responseBody =
                mockMvc.perform(
                                get(
                                        "/api/players/{playerId}/teammates",
                                        michaelJordan.getId()
                                )
                                        .with(authentication(authentication))
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json =
                objectMapper.readTree(responseBody);

        String jsonText =
                json.toString();

        org.assertj.core.api.Assertions.assertThat(jsonText)
                .contains("Isiah")
                .contains("Thomas")
                .contains("Dennis")
                .contains("Rodman")
                .doesNotContain("\"firstName\":\"Michael\"");
    }


    @Test
    void shouldReturnBadRequestWhenFreeAgentRequestsTeammates() throws Exception {

        Player freeAgent =
                createAndSavePlayer(
                        "Kobe",
                        "Bryant",
                        null
                );

        entityManager.flush();
        entityManager.clear();

        Authentication authentication =
                createUserAuthentication(
                        "players.freeagent.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get(
                                "/api/players/{playerId}/teammates",
                                freeAgent.getId()
                        )
                                .with(authentication(authentication))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnNotFoundWhenGettingTeammatesForMissingPlayer() throws Exception {

        Authentication authentication =
                createUserAuthentication(
                        "players.missing.teammates.user",
                        UserRole.ROLE_USER
                );

        mockMvc.perform(
                        get(
                                "/api/players/{playerId}/teammates",
                                999999L
                        )
                                .with(authentication(authentication))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnUnauthorizedWhenGettingTeammatesWithoutAuthentication() throws Exception {

        SecurityContextHolder.clearContext();

        mockMvc.perform(
                        get(
                                "/api/players/{playerId}/teammates",
                                1L
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }


    // ============================================================
    // Helpers
    // ============================================================

    private Authentication createUserAuthentication(
            String username,
            UserRole role
    ) {

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .email(username + "@example.com")
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
                .playerPositions(
                        new HashSet<>(
                                Set.of(PlayerPosition.PG)
                        )
                )
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


    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}

