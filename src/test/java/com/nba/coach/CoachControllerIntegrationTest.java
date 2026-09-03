package com.nba.coach;

import com.nba.AbstractIntegrationTest;
import com.nba.security.CustomUserDetails;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CoachControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private MockHttpSession authenticatedSession;

    @BeforeEach
    void setUp() {
        authenticatedSession = createAuthenticatedSession();
    }

    // =========================================================
    // GET COACH BY ID
    // =========================================================

    @Test
    void shouldReturnCoachById() throws Exception {
        String unique = unique();

        Team team = createTeam(
                "Controller Get Team " + unique
        );

        Coach coach = createCoach(
                "John_" + unique,
                "Smith_" + unique,
                12,
                team
        );

        coach.setSalary(
                new BigDecimal("4500000.50")
        );

        coach.setChampionshipsWon(8);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/{id}", coach.getId())
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id")
                        .value(coach.getId()))
                .andExpect(jsonPath("$.firstName")
                        .value("John_" + unique))
                .andExpect(jsonPath("$.lastName")
                        .value("Smith_" + unique))
                .andExpect(jsonPath("$.teamRole")
                        .value("Coach"))
                .andExpect(jsonPath("$.teamName")
                        .value(team.getName()))
                .andExpect(jsonPath("$.yearsExperience")
                        .value(12))
                .andExpect(jsonPath("$.championshipWon")
                        .value(8))
                .andExpect(jsonPath("$.salary")
                        .value(4500000.50));
    }

    @Test
    void shouldReturnFreeAgentCoachById() throws Exception {
        String unique = unique();

        Coach coach = createCoach(
                "Free_" + unique,
                "Agent_" + unique,
                5,
                null
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/{id}", coach.getId())
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.id")
                        .value(coach.getId()))
                .andExpect(jsonPath("$.firstName")
                        .value("Free_" + unique))
                .andExpect(jsonPath("$.lastName")
                        .value("Agent_" + unique))
                .andExpect(jsonPath("$.teamRole")
                        .value("Coach"))
                .andExpect(jsonPath("$.teamName")
                        .doesNotExist());
    }

    @Test
    void shouldReturnNotFoundWhenCoachDoesNotExist() throws Exception {
        mockMvc.perform(
                        get("/api/coaches/{id}", 999999L)
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenCoachIdIsInvalid() throws Exception {
        mockMvc.perform(
                        get("/api/coaches/{id}", "abc")
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnauthenticatedGetCoachById() throws Exception {
        mockMvc.perform(
                        get("/api/coaches/{id}", 999999L)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // GET ALL COACHES
    // =========================================================

    @Test
    void shouldReturnPaginatedCoaches() throws Exception {
        String unique = unique();

        Team team = createTeam(
                "Controller Pagination Team " + unique
        );

        createCoach(
                "PaginationOne_" + unique,
                "CoachOne_" + unique,
                10,
                team
        );

        createCoach(
                "PaginationTwo_" + unique,
                "CoachTwo_" + unique,
                11,
                team
        );

        createCoach(
                "PaginationThree_" + unique,
                "CoachThree_" + unique,
                12,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches")
                                .session(authenticatedSession)
                                .param("page", "0")
                                .param("size", "2")
                                .param("sort", "id,asc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.content")
                        .isArray())
                .andExpect(jsonPath("$.content.length()")
                        .value(2))
                .andExpect(jsonPath("$.totalElements")
                        .value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.size")
                        .value(2))
                .andExpect(jsonPath("$.number")
                        .value(0));
    }

    @Test
    void shouldRespectPageAndSizeParameters() throws Exception {
        String unique = unique();

        Team team = createTeam(
                "Controller Page Parameters " + unique
        );

        createCoach(
                "PageOne_" + unique,
                "CoachOne_" + unique,
                20,
                team
        );

        createCoach(
                "PageTwo_" + unique,
                "CoachTwo_" + unique,
                21,
                team
        );

        createCoach(
                "PageThree_" + unique,
                "CoachThree_" + unique,
                22,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches")
                                .session(authenticatedSession)
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "id,asc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.size")
                        .value(1))
                .andExpect(jsonPath("$.number")
                        .value(1));
    }

    @Test
    void shouldUseDefaultPageWhenNegativePageIsProvided()
            throws Exception {

        mockMvc.perform(
                        get("/api/coaches")
                                .session(authenticatedSession)
                                .param("page", "-1")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(10));
    }

    @Test
    void shouldUseDefaultPageSizeWhenZeroIsProvided()
            throws Exception {

        mockMvc.perform(
                        get("/api/coaches")
                                .session(authenticatedSession)
                                .param("page", "0")
                                .param("size", "0")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(20));
    }

    @Test
    void shouldUseDefaultPageSizeWhenNonNumericValueIsProvided()
            throws Exception {

        mockMvc.perform(
                        get("/api/coaches")
                                .session(authenticatedSession)
                                .param("page", "0")
                                .param("size", "abc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(20));
    }

    @Test
    void shouldRejectUnauthenticatedGetAllCoaches() throws Exception {
        mockMvc.perform(
                        get("/api/coaches")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Test
    void shouldSearchCoachesByFirstName() throws Exception {
        String unique = unique();

        String firstName =
                "ControllerSearch_" + unique;

        Team team = createTeam(
                "Controller Search Team " + unique
        );

        Coach matching = createCoach(
                firstName,
                "Matching_" + unique,
                30,
                team
        );

        createCoach(
                "Other_" + unique,
                "Other_" + unique,
                31,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param("firstName", firstName)
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(matching.getId()))
                .andExpect(jsonPath("$.content[0].firstName")
                        .value(firstName));
    }

    @Test
    void shouldSearchCoachesByLastNameIgnoringCase() throws Exception {
        String unique = unique();

        String lastName =
                "ControllerLastName_" + unique;

        Team team = createTeam(
                "Controller Last Name Team " + unique
        );

        Coach matching = createCoach(
                "First_" + unique,
                lastName,
                32,
                team
        );

        createCoach(
                "Other_" + unique,
                "Different_" + unique,
                33,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param(
                                        "lastName",
                                        lastName.toUpperCase()
                                )
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(matching.getId()));
    }

    @Test
    void shouldSearchCoachesByTeamName() throws Exception {
        String unique = unique();

        String teamName =
                "ControllerUniqueTeam_" + unique;

        Team matchingTeam =
                createTeam(teamName);

        Team otherTeam =
                createTeam(
                        "ControllerOtherTeam_" + unique
                );

        Coach matching = createCoach(
                "TeamSearch_" + unique,
                "Coach_" + unique,
                34,
                matchingTeam
        );

        createCoach(
                "Other_" + unique,
                "Coach_" + unique,
                35,
                otherTeam
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param(
                                        "teamName",
                                        teamName.toLowerCase()
                                )
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(matching.getId()))
                .andExpect(jsonPath("$.content[0].teamName")
                        .value(teamName));
    }

    @Test
    void shouldSearchCoachesByExperienceRange() throws Exception {
        String unique = unique();

        String lastName =
                "ControllerExperience_" + unique;

        Team team = createTeam(
                "Controller Experience Team " + unique
        );

        Coach matching = createCoach(
                "Experience_" + unique,
                lastName,
                9000,
                team
        );

        createCoach(
                "Outside_" + unique,
                "Outside_" + unique,
                100,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param("lastName", lastName)
                                .param("minYearsOfExperience", "9000")
                                .param("maxYearsOfExperience", "9000")
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(matching.getId()))
                .andExpect(jsonPath("$.content[0].yearsExperience")
                        .value(9000));
    }

    @Test
    void shouldSearchCoachesBySalaryRange() throws Exception {
        String unique = unique();

        String lastName =
                "ControllerSalary_" + unique;

        Team team = createTeam(
                "Controller Salary Team " + unique
        );

        Coach matching = createCoach(
                "Salary_" + unique,
                lastName,
                40,
                team
        );

        matching.setSalary(
                new BigDecimal("900000000.00")
        );

        createCoach(
                "Outside_" + unique,
                "OutsideSalary_" + unique,
                41,
                team
        ).setSalary(
                new BigDecimal("1000.00")
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param("lastName", lastName)
                                .param(
                                        "minSalary",
                                        "900000000.00"
                                )
                                .param(
                                        "maxSalary",
                                        "900000000.00"
                                )
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(matching.getId()))
                .andExpect(jsonPath("$.content[0].salary")
                        .value(900000000.00));
    }

    @Test
    void shouldSearchCoachesByChampionshipsRange() throws Exception {
        String unique = unique();

        String lastName =
                "ControllerChampionship_" + unique;

        Team team = createTeam(
                "Controller Championship Team " + unique
        );

        Coach matching = createCoach(
                "Championship_" + unique,
                lastName,
                42,
                team
        );

        matching.setChampionshipsWon(9000);

        createCoach(
                "Outside_" + unique,
                "OutsideChampionship_" + unique,
                43,
                team
        ).setChampionshipsWon(5);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param("lastName", lastName)
                                .param(
                                        "minChampionshipsWon",
                                        "9000"
                                )
                                .param(
                                        "maxChampionshipsWon",
                                        "9000"
                                )
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(matching.getId()))
                .andExpect(jsonPath("$.content[0].championshipWon")
                        .value(9000));
    }

    @Test
    void shouldSearchCoachesUsingMultipleFilters() throws Exception {
        String unique = unique();

        String firstName =
                "MultiFirst_" + unique;

        String lastName =
                "MultiLast_" + unique;

        String teamName =
                "MultiTeam_" + unique;

        Team matchingTeam =
                createTeam(teamName);

        Team otherTeam =
                createTeam(
                        "OtherMultiTeam_" + unique
                );

        Coach matching = createCoach(
                firstName,
                lastName,
                9000,
                matchingTeam
        );

        matching.setSalary(
                new BigDecimal("900000000.00")
        );

        matching.setChampionshipsWon(9000);

        createCoach(
                "WrongFirst_" + unique,
                lastName,
                9000,
                matchingTeam
        ).setSalary(
                new BigDecimal("900000000.00")
        );

        createCoach(
                firstName,
                "WrongLast_" + unique,
                9000,
                otherTeam
        ).setSalary(
                new BigDecimal("900000000.00")
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param("firstName", firstName)
                                .param("lastName", lastName)
                                .param("teamName", teamName)
                                .param(
                                        "minChampionshipsWon",
                                        "9000"
                                )
                                .param(
                                        "maxChampionshipsWon",
                                        "9000"
                                )
                                .param(
                                        "minYearsOfExperience",
                                        "9000"
                                )
                                .param(
                                        "maxYearsOfExperience",
                                        "9000"
                                )
                                .param(
                                        "minSalary",
                                        "900000000.00"
                                )
                                .param(
                                        "maxSalary",
                                        "900000000.00"
                                )
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(matching.getId()))
                .andExpect(jsonPath("$.content[0].firstName")
                        .value(firstName))
                .andExpect(jsonPath("$.content[0].lastName")
                        .value(lastName))
                .andExpect(jsonPath("$.content[0].teamName")
                        .value(teamName));
    }

    @Test
    void shouldReturnAllCoachesWhenSearchHasNoParameters()
            throws Exception {

        String unique = unique();

        Team team = createTeam(
                "Controller Empty Search Team " + unique
        );

        createCoach(
                "EmptySearchOne_" + unique,
                "CoachOne_" + unique,
                50,
                team
        );

        createCoach(
                "EmptySearchTwo_" + unique,
                "CoachTwo_" + unique,
                51,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param("page", "0")
                                .param("size", "1000")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .isArray())
                .andExpect(jsonPath("$.totalElements")
                        .value(greaterThanOrEqualTo(2)));
    }

    @Test
    void shouldReturnEmptySearchResult() throws Exception {
        String unique = unique();

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param(
                                        "firstName",
                                        "NO_MATCH_" + unique
                                )
                                .param("page", "0")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .isEmpty())
                .andExpect(jsonPath("$.totalElements")
                        .value(0));
    }

    @Test
    void shouldUseDefaultSearchPageWhenNegativePageIsProvided()
            throws Exception {

        mockMvc.perform(
                        get("/api/coaches/search")
                                .session(authenticatedSession)
                                .param("page", "-1")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(20));
    }

    @Test
    void shouldRejectUnauthenticatedSearchRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/coaches/search")
                                .param("firstName", "anything")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // COLLEAGUES
    // =========================================================

    @Test
    void shouldReturnCoachColleagues() throws Exception {
        String unique = unique();

        Team team = createTeam(
                "Controller Colleagues Team " + unique
        );

        Coach mainCoach = createCoach(
                "Main_" + unique,
                "Coach_" + unique,
                60,
                team
        );

        Coach firstColleague = createCoach(
                "First_" + unique,
                "Assistant_" + unique,
                61,
                team
        );

        Coach secondColleague = createCoach(
                "Second_" + unique,
                "Assistant_" + unique,
                62,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get(
                                "/api/coaches/{coachId}/colleagues",
                                mainCoach.getId()
                        )
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(jsonPath("$.teamName")
                        .value(team.getName()))
                .andExpect(jsonPath("$.title")
                        .value(
                                "COLLEAGUES OF Main_" + unique
                                        + " Coach_" + unique
                        ))
                .andExpect(jsonPath("$.members")
                        .isArray())
                .andExpect(jsonPath("$.members.length()")
                        .value(2))
                .andExpect(
                        jsonPath(
                                "$.members[*].id",
                                hasItems(
                                        firstColleague
                                                .getId()
                                                .intValue(),
                                        secondColleague
                                                .getId()
                                                .intValue()
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.members[*].fullName",
                                hasItems(
                                        "First_" + unique
                                                + " Assistant_" + unique,
                                        "Second_" + unique
                                                + " Assistant_" + unique
                                )
                        )
                );
    }

    @Test
    void shouldReturnEmptyColleaguesForOnlyCoach() throws Exception {
        String unique = unique();

        Team team = createTeam(
                "Controller Only Coach Team " + unique
        );

        Coach coach = createCoach(
                "Only_" + unique,
                "Coach_" + unique,
                63,
                team
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get(
                                "/api/coaches/{coachId}/colleagues",
                                coach.getId()
                        )
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName")
                        .value(team.getName()))
                .andExpect(jsonPath("$.members")
                        .isArray())
                .andExpect(jsonPath("$.members.length()")
                        .value(0));
    }

    @Test
    void shouldReturnBadRequestWhenFreeAgentRequestsColleagues()
            throws Exception {

        String unique = unique();

        Coach freeAgent = createCoach(
                "Free_" + unique,
                "Agent_" + unique,
                64,
                null
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get(
                                "/api/coaches/{coachId}/colleagues",
                                freeAgent.getId()
                        )
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenColleagueCoachDoesNotExist()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/coaches/{coachId}/colleagues",
                                999999L
                        )
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenColleagueCoachIdIsInvalid()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/coaches/{coachId}/colleagues",
                                "abc"
                        )
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnauthenticatedColleaguesRequest()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/coaches/{coachId}/colleagues",
                                999999L
                        )
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // AUTHENTICATION
    // =========================================================

    @Test
    void shouldAllowAuthenticatedUserToAccessCoachEndpoint()
            throws Exception {

        String unique = unique();

        Coach coach = createCoach(
                "Authenticated_" + unique,
                "Coach_" + unique,
                70,
                null
        );

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/api/coaches/{id}", coach.getId())
                                .session(authenticatedSession)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private MockHttpSession createAuthenticatedSession() {
        String unique = unique();

        User user = User.builder()
                .username("test_user_" + unique)
                .password("test_password")
                .email("test_" + unique + "@example.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .registeredAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails =
                new CustomUserDetails(savedUser);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        MockHttpSession session = new MockHttpSession();

        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        return session;
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

