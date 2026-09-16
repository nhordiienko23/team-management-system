package com.nba.player;

import com.nba.AbstractIntegrationTest;
import com.nba.core.exception.notFound.PlayerNotFoundException;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager entityManager;


    // ============================================================
    // findAllByTeamId
    // ============================================================

    @Test
    void shouldFindAllPlayersByTeamId() {

        Team team = createAndSaveTeam(
                "repository_players_team_test",
                5,
                1980
        );

        Player firstPlayer = createAndSavePlayer(
                "LeBron",
                "James",
                team
        );

        Player secondPlayer = createAndSavePlayer(
                "Anthony",
                "Davis",
                team
        );

        Player thirdPlayer = createAndSavePlayer(
                "Austin",
                "Reaves",
                team
        );

        Team anotherTeam = createAndSaveTeam(
                "repository_other_team_test",
                2,
                1990
        );

        Player playerFromAnotherTeam = createAndSavePlayer(
                "Stephen",
                "Curry",
                anotherTeam
        );

        entityManager.flush();
        entityManager.clear();

        var players =
                playerRepository.findAllByTeamId(team.getId());

        assertThat(players)
                .hasSize(3);

        assertThat(players)
                .extracting(Player::getId)
                .containsExactlyInAnyOrder(
                        firstPlayer.getId(),
                        secondPlayer.getId(),
                        thirdPlayer.getId()
                );

        assertThat(players)
                .extracting(Player::getId)
                .doesNotContain(playerFromAnotherTeam.getId());

        assertThat(players)
                .allSatisfy(player -> {
                    assertThat(player.getTeam())
                            .isNotNull();

                    assertThat(player.getTeam().getId())
                            .isEqualTo(team.getId());

                    assertThat(player.getPlayerPositions())
                            .isNotEmpty();
                });
    }


    @Test
    void shouldReturnEmptyListWhenTeamHasNoPlayers() {

        Team team = createAndSaveTeam(
                "repository_empty_players_team_test",
                0,
                2020
        );

        entityManager.flush();
        entityManager.clear();

        var players =
                playerRepository.findAllByTeamId(team.getId());

        assertThat(players)
                .isEmpty();
    }


    @Test
    void shouldReturnEmptyListWhenTeamDoesNotExist() {

        var players =
                playerRepository.findAllByTeamId(999999L);

        assertThat(players)
                .isEmpty();
    }


    // ============================================================
    // findPlayerWithTeamAndPositionsById
    // ============================================================

    @Test
    void shouldFindPlayerWithTeamAndPositionsById() {

        Team team = createAndSaveTeam(
                "repository_find_player_team_test",
                7,
                1990
        );

        Player player = createAndSavePlayer(
                "Luka",
                "Doncic",
                team
        );

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository
                        .findPlayerWithTeamAndPositionsById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getId())
                .isEqualTo(player.getId());

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("Luka");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("Doncic");

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactly(PlayerPosition.PG);

        assertThat(actualPlayer.getRating())
                .isEqualTo(90);
    }


    @Test
    void shouldReturnEmptyWhenPlayerDoesNotExist() {

        var result =
                playerRepository.findPlayerWithTeamAndPositionsById(
                        999999L
                );

        assertThat(result)
                .isEmpty();
    }


    // ============================================================
    // findByIdAndTeamId
    // ============================================================

    @Test
    void shouldFindPlayerByIdAndTeamId() {

        Team team = createAndSaveTeam(
                "repository_find_by_team_test",
                8,
                1995
        );

        Player player = createAndSavePlayer(
                "Nikola",
                "Jokic",
                team
        );

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository
                        .findByIdAndTeamId(
                                player.getId(),
                                team.getId()
                        )
                        .orElseThrow();

        assertThat(actualPlayer.getId())
                .isEqualTo(player.getId());

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactly(PlayerPosition.PG);
    }


    @Test
    void shouldReturnEmptyWhenPlayerBelongsToAnotherTeam() {

        Team actualTeam = createAndSaveTeam(
                "repository_actual_player_team_test",
                8,
                1995
        );

        Team requestedTeam = createAndSaveTeam(
                "repository_requested_player_team_test",
                5,
                2000
        );

        Player player = createAndSavePlayer(
                "Nikola",
                "Jokic",
                actualTeam
        );

        entityManager.flush();
        entityManager.clear();

        var result =
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        requestedTeam.getId()
                );

        assertThat(result)
                .isEmpty();
    }


    @Test
    void shouldReturnEmptyWhenPlayerDoesNotExistForTeam() {

        Team team = createAndSaveTeam(
                "repository_missing_player_team_test",
                5,
                2000
        );

        var result =
                playerRepository.findByIdAndTeamId(
                        999999L,
                        team.getId()
                );

        assertThat(result)
                .isEmpty();
    }


    // ============================================================
    // getPlayerByIdOrThrow404
    // ============================================================

    @Test
    void shouldReturnPlayerByIdOrThrow404() {

        Team team = createAndSaveTeam(
                "repository_throw_player_team_test",
                10,
                2005
        );

        Player player = createAndSavePlayer(
                "Jayson",
                "Tatum",
                team
        );

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.getPlayerByIdOrThrow404(
                        player.getId()
                );

        assertThat(actualPlayer.getId())
                .isEqualTo(player.getId());

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("Jayson");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("Tatum");

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactly(PlayerPosition.PG);
    }


    @Test
    void shouldThrowPlayerNotFoundExceptionWhenPlayerDoesNotExist() {

        assertThatThrownBy(() ->
                playerRepository.getPlayerByIdOrThrow404(999999L)
        )
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void shouldFindPlayersWithPagination() {

        Team team = createAndSaveTeam(
                "repository_pagination_team_test",
                5,
                1980
        );

        for (int i = 1; i <= 5; i++) {
            createAndSavePlayer(
                    "Player",
                    "Pagination" + i,
                    team
            );
        }

        entityManager.flush();
        entityManager.clear();

        Page<Player> result =
                playerRepository.findAll(
                        PageRequest.of(0, 2)
                );

        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getTotalElements())
                .isGreaterThanOrEqualTo(5);

        assertThat(result.getTotalPages())
                .isGreaterThanOrEqualTo(3);
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
}

