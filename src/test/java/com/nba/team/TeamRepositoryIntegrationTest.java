package com.nba.team;

import com.nba.AbstractIntegrationTest;
import com.nba.coach.Coach;
import com.nba.coach.CoachRepository;
import com.nba.player.Player;
import com.nba.player.PlayerPosition;
import com.nba.player.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TeamRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CoachRepository coachRepository;


    // ============================================================
    // findAll(Pageable)
    // ============================================================

    @Test
    void shouldReturnPaginatedTeams() {

        Team team1 = createAndSaveTeam(
                "repository_test_team_1",
                3,
                1990
        );

        Team team2 = createAndSaveTeam(
                "repository_test_team_2",
                5,
                1995
        );
        long totalCountInDB = teamRepository.count();
        Pageable pageable = PageRequest.of(0, 2);

        Page<Team> teamPage = teamRepository.findAll(pageable);

        assertThat(teamPage).isNotNull();
        assertThat(teamPage.getContent())
                .hasSize(2);
        assertThat(teamPage.getNumber())
                .isZero();
        assertThat(teamPage.getTotalElements())
                .isEqualTo(totalCountInDB);
        assertThat(teamPage.getSize())
                .isEqualTo(2);
    }


    @Test
    void shouldReturnSecondPageOfTeams() {

        createAndSaveTeam(
                "repository_page_team_1",
                1,
                1990
        );

        createAndSaveTeam(
                "repository_page_team_2",
                2,
                1991
        );

        createAndSaveTeam(
                "repository_page_team_3",
                3,
                1992
        );

        Pageable pageable = PageRequest.of(1, 2);

        Page<Team> teamPage = teamRepository.findAll(pageable);

        assertThat(teamPage).isNotNull();
        assertThat(teamPage.getNumber()).isEqualTo(1);
        assertThat(teamPage.getSize()).isEqualTo(2);
        assertThat(teamPage.getContent()).hasSize(2);
    }


    // ============================================================
    // existsByName(String)
    // ============================================================

    @Test
    void shouldReturnTrueWhenTeamWithNameExists() {

        String teamName = "repository_exists_team";

        createAndSaveTeam(
                teamName,
                4,
                2000
        );

        assertThat(teamRepository.existsByName(teamName))
                .isTrue();
    }


    @Test
    void shouldReturnFalseWhenTeamWithNameDoesNotExist() {

        assertThat(teamRepository.existsByName("repository_missing_team"))
                .isFalse();
    }


    // ============================================================
    // findByIdWithTeamMembers(Long)
    // ============================================================

    @Test
    void shouldReturnTeamWithPlayersAndCoaches() {

        Team team = createAndSaveTeam(
                "repository_members_team",
                10,
                1985
        );

        Player player = createAndSavePlayer(
                "Nikita",
                "Player",
                team
        );

        Coach coach = createAndSaveCoach(
                "Svitlana",
                "Coach",
                team
        );

        Team foundTeam = teamRepository
                .findByIdWithTeamMembers(team.getId())
                .orElseThrow();

        assertThat(foundTeam.getId())
                .isEqualTo(team.getId());

        assertThat(foundTeam.getName())
                .isEqualTo("repository_members_team");

        assertThat(foundTeam.getTeamMembers())
                .hasSize(2);

        assertThat(foundTeam.getTeamMembers())
                .contains(player, coach);

        assertThat(foundTeam.getPlayers())
                .hasSize(1);

        assertThat(foundTeam.getPlayers().get(0).getFirstName())
                .isEqualTo("Nikita");

        assertThat(foundTeam.getCoaches())
                .hasSize(1);

        assertThat(foundTeam.getCoaches().get(0).getFirstName())
                .isEqualTo("Svitlana");
    }


    @Test
    void shouldReturnTeamWithoutMembersWhenTeamHasNoMembers() {

        Team team = createAndSaveTeam(
                "repository_empty_team",
                0,
                1999
        );

        Team foundTeam = teamRepository
                .findByIdWithTeamMembers(team.getId())
                .orElseThrow();

        assertThat(foundTeam.getTeamMembers())
                .isEmpty();
    }


    @Test
    void shouldReturnEmptyWhenTeamDoesNotExist() {

        Long nonExistingTeamId = 999999L;

        assertThat(
                teamRepository.findByIdWithTeamMembers(nonExistingTeamId)
        ).isEmpty();
    }


    // ============================================================
    // getTeamByIdOrThrow404(Long)
    // ============================================================

    @Test
    void shouldReturnTeamById() {

        Team team = createAndSaveTeam(
                "repository_get_team",
                7,
                1980
        );

        Team foundTeam = teamRepository.getTeamByIdOrThrow404(team.getId());

        assertThat(foundTeam).isNotNull();
        assertThat(foundTeam.getId())
                .isEqualTo(team.getId());

        assertThat(foundTeam.getName())
                .isEqualTo("repository_get_team");

        assertThat(foundTeam.getChampionshipTitleCount())
                .isEqualTo(7);

        assertThat(foundTeam.getCreationYear())
                .isEqualTo(1980);
    }


    @Test
    void shouldThrowTeamNotFoundExceptionWhenTeamDoesNotExist() {

        Long nonExistingTeamId = 999999L;

        assertThatThrownBy(() ->
                teamRepository.getTeamByIdOrThrow404(nonExistingTeamId)
        )
                .isInstanceOf(
                        com.nba.core.exception.notFound.TeamNotFoundException.class
                );
    }


    // ============================================================
    // Helpers
    // ============================================================

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

        Player savedPlayer = playerRepository.save(player);

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

        Coach savedCoach = coachRepository.save(coach);
        team.addTeamMember(savedCoach);

        return savedCoach;
    }
}