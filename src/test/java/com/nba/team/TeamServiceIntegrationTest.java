package com.nba.team;

import com.nba.AbstractIntegrationTest;
import com.nba.coach.Coach;
import com.nba.coach.CoachRepository;
import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.core.exception.invalidData.InvalidTeamDataException;
import com.nba.core.exception.notFound.TeamNotFoundException;
import com.nba.player.Player;
import com.nba.player.PlayerPosition;
import com.nba.player.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@ActiveProfiles("test")
@Transactional
class TeamServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CoachRepository coachRepository;


    // ============================================================
    // addTeam()
    // ============================================================

    @Test
    void shouldCreateTeam() {

        RequestTeamDto request = new RequestTeamDto(
                "lakers_test",
                17,
                1947
        );

        ResponseTeamDto response = teamService.addTeam(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("lakers_test");
        assertThat(response.championshipTitleCount()).isEqualTo(17);
        assertThat(response.creationYear()).isEqualTo(1947);
        assertThat(response.players()).isEmpty();
        assertThat(response.coaches()).isEmpty();

        Team savedTeam = teamRepository.findById(response.id())
                .orElseThrow();

        assertThat(savedTeam.getName()).isEqualTo("lakers_test");
        assertThat(savedTeam.getChampionshipTitleCount()).isEqualTo(17);
        assertThat(savedTeam.getCreationYear()).isEqualTo(1947);
    }


    @Test
    void shouldThrowExceptionWhenCreatingTeamWithExistingName() {

        createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        RequestTeamDto request = new RequestTeamDto(
                "lakers_test",
                10,
                2000
        );

        assertThatThrownBy(() -> teamService.addTeam(request))
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage("Team with teamName lakers_test already exists");
    }


    // ============================================================
    // getAllTeams()
    // ============================================================

    @Test
    void shouldReturnAllTeamsWithPagination() {

        Team team1 = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team team2 = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        Pageable pageable = PageRequest.of(0, 100);

        Page<ResponseTeamDto> result =
                teamService.getAllTeams(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(team1.getName(), team2.getName());
    }


    @Test
    void shouldReturnEmptyPageWhenRequestedPageIsOutOfRange() {

        Pageable pageable = PageRequest.of(1000, 10);

        Page<ResponseTeamDto> result =
                teamService.getAllTeams(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }


    // ============================================================
    // partialUpdateTeam()
    // ============================================================

    @Test
    void shouldPartiallyUpdateTeamName() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "warriors_test",
                null,
                null
        );

        ResponseTeamDto response =
                teamService.partialUpdateTeam(team.getId(), request);

        assertThat(response.name()).isEqualTo("warriors_test");
        assertThat(response.championshipTitleCount()).isEqualTo(17);
        assertThat(response.creationYear()).isEqualTo(1947);

        Team updatedTeam = teamRepository
                .findById(team.getId())
                .orElseThrow();

        assertThat(updatedTeam.getName())
                .isEqualTo("warriors_test");
    }


    @Test
    void shouldPartiallyUpdateChampionshipCount() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        PatchTeamRequest request = new PatchTeamRequest(
                null,
                20,
                null
        );

        ResponseTeamDto response =
                teamService.partialUpdateTeam(team.getId(), request);

        assertThat(response.name()).isEqualTo("lakers_test");
        assertThat(response.championshipTitleCount()).isEqualTo(20);
        assertThat(response.creationYear()).isEqualTo(1947);

        Team updatedTeam = teamRepository
                .findById(team.getId())
                .orElseThrow();

        assertThat(updatedTeam.getChampionshipTitleCount())
                .isEqualTo(20);
    }


    @Test
    void shouldPartiallyUpdateCreationYear() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        PatchTeamRequest request = new PatchTeamRequest(
                null,
                null,
                2000
        );

        ResponseTeamDto response =
                teamService.partialUpdateTeam(
                        team.getId(),
                        request
                );

        assertThat(response.name())
                .isEqualTo("lakers_test");

        assertThat(response.championshipTitleCount())
                .isEqualTo(17);

        assertThat(response.creationYear())
                .isEqualTo(2000);

        Team updatedTeam =
                teamRepository.findById(team.getId())
                        .orElseThrow();

        assertThat(updatedTeam.getCreationYear())
                .isEqualTo(2000);
    }


    @Test
    void shouldPartiallyUpdateTeamNameAndChampionshipCount() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "warriors_test",
                7,
                null
        );

        ResponseTeamDto response =
                teamService.partialUpdateTeam(team.getId(), request);

        assertThat(response.name()).isEqualTo("warriors_test");
        assertThat(response.championshipTitleCount()).isEqualTo(7);
        assertThat(response.creationYear()).isEqualTo(1947);
    }


    @Test
    void shouldNotChangeAnythingWhenPatchContainsOnlyNulls() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        PatchTeamRequest request =
                new PatchTeamRequest(null, null, null);

        ResponseTeamDto response =
                teamService.partialUpdateTeam(team.getId(), request);

        assertThat(response.name()).isEqualTo("lakers_test");
        assertThat(response.championshipTitleCount()).isEqualTo(17);
        assertThat(response.creationYear()).isEqualTo(1947);
    }


    @Test
    void shouldThrowExceptionWhenUpdatingTeamToExistingName() {

        createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team team2 = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "lakers_test",
                null,
                null
        );

        assertThatThrownBy(() ->
                teamService.partialUpdateTeam(team2.getId(), request)
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage("Team with teamName lakers_test already exists");
    }


    @Test
    void shouldAllowKeepingSameTeamName() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        PatchTeamRequest request = new PatchTeamRequest(
                "lakers_test",
                null,
                null
        );

        ResponseTeamDto response =
                teamService.partialUpdateTeam(team.getId(), request);

        assertThat(response.name()).isEqualTo("lakers_test");
    }


    @Test
    void shouldThrowTeamNotFoundWhenUpdatingNonExistingTeam() {

        PatchTeamRequest request = new PatchTeamRequest(
                "warriors_test",
                5,
                null
        );

        Long nonExistingTeamId = 999999L;

        assertThatThrownBy(() ->
                teamService.partialUpdateTeam(nonExistingTeamId, request)
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    // ============================================================
    // getTeamById()
    // ============================================================

    @Test
    void shouldReturnTeamById() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        ResponseTeamDto response =
                teamService.getTeamById(team.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(team.getId());
        assertThat(response.name()).isEqualTo("lakers_test");
        assertThat(response.championshipTitleCount()).isEqualTo(17);
        assertThat(response.creationYear()).isEqualTo(1947);
        assertThat(response.players()).isEmpty();
        assertThat(response.coaches()).isEmpty();
    }


    @Test
    void shouldReturnTeamWithPlayersAndCoaches() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "nikita_test",
                "player_test",
                team
        );

        Coach coach = createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team
        );

        ResponseTeamDto response =
                teamService.getTeamById(team.getId());

        assertThat(response.players())
                .hasSize(1);

        assertThat(response.coaches())
                .hasSize(1);

        assertThat(response.players().get(0).fullName())
                .isEqualTo("nikita_test player_test");

        assertThat(response.coaches().get(0).fullName())
                .isEqualTo("sveta_test coach_test");
    }


    @Test
    void shouldThrowTeamNotFoundWhenGettingNonExistingTeam() {

        assertThatThrownBy(() ->
                teamService.getTeamById(999999L)
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    // ============================================================
    // deleteTeam()
    // ============================================================

    @Test
    void shouldDeleteTeamWithoutMembers() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Long teamId = team.getId();

        teamService.deleteTeam(teamId);

        assertThat(teamRepository.findById(teamId))
                .isEmpty();
    }


    @Test
    void shouldDeleteTeamWithPlayersAndCoaches() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "nikita_test",
                "player_test",
                team
        );

        Coach coach = createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team
        );

        Long teamId = team.getId();
        Long playerId = player.getId();
        Long coachId = coach.getId();

        teamService.deleteTeam(teamId);

        assertThat(teamRepository.findById(teamId))
                .isEmpty();

        Player deletedPlayer = playerRepository
                .findById(playerId)
                .orElseThrow();

        Coach deletedCoach = coachRepository
                .findById(coachId)
                .orElseThrow();

        assertThat(deletedPlayer.getTeam())
                .isNull();

        assertThat(deletedCoach.getTeam())
                .isNull();
    }


    @Test
    void shouldThrowTeamNotFoundWhenDeletingNonExistingTeam() {

        assertThatThrownBy(() ->
                teamService.deleteTeam(999999L)
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    // ============================================================
    // searchTeams()
    // ============================================================

    @Test
    void shouldReturnTeamsWithoutFilters() {

        Team team1 = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team team2 = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(null, null, null, null, null);

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result).isNotNull();

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(team1.getName(), team2.getName());
    }


    @Test
    void shouldSearchTeamsByName() {

        Team lakers = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team bulls = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        "LAKERS",
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(lakers.getName());

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .doesNotContain(bulls.getName());
    }


    @Test
    void shouldSearchTeamsByMinimumChampionshipCount() {

        Team lakers = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        null,
                        10,
                        null,
                        null,
                        null
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(lakers.getName());
    }


    @Test
    void shouldSearchTeamsByMaximumChampionshipCount() {

        createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team bulls = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        null,
                        null,
                        10,
                        null,
                        null
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(bulls.getName());
    }


    @Test
    void shouldSearchTeamsByMinimumCreationYear() {

        Team target = createAndSaveTeam(
                "lakers_test",
                17,
                1990
        );

        createAndSaveTeam(
                "bulls_test",
                6,
                1960
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        null,
                        null,
                        null,
                        1980,
                        null
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(target.getName());
    }


    @Test
    void shouldSearchTeamsByMaximumCreationYear() {

        createAndSaveTeam(
                "lakers_test",
                17,
                1990
        );

        Team target = createAndSaveTeam(
                "bulls_test",
                6,
                1960
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        null,
                        null,
                        null,
                        null,
                        1980
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(target.getName());
    }


    @Test
    void shouldSearchTeamsByCreationYearRange() {

        Team lakers = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        null,
                        null,
                        null,
                        1940,
                        1950
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(lakers.getName());
    }


    @Test
    void shouldSearchTeamsUsingMultipleFilters() {

        Team target = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        "lakers",
                        10,
                        20,
                        1940,
                        1950
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result.getContent())
                .extracting(ResponseTeamDto::name)
                .contains(target.getName());
    }


    @Test
    void shouldReturnEmptyPageWhenNoTeamsMatchSearchFilter() {

        createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        "non_existing_team",
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        PageRequest.of(0, 100)
                );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }


    @Test
    void shouldReturnPaginatedSearchResults() {

        createAndSaveTeam(
                "pagination_test_team_1",
                17,
                1947
        );

        createAndSaveTeam(
                "pagination_test_team_2",
                15,
                1950
        );

        createAndSaveTeam(
                "pagination_test_team_3",
                12,
                1960
        );

        TeamSearchFilter filter =
                new TeamSearchFilter(
                        "pagination_test",
                        null,
                        null,
                        null,
                        null
                );

        Pageable pageable = PageRequest.of(1, 2);

        Page<ResponseTeamDto> result =
                teamService.searchTeams(
                        filter,
                        pageable
                );

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(1);
    }


    // ============================================================
    // addPlayerToTeam()
    // ============================================================

    @Test
    void shouldAddFreeAgentPlayerToTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Player player = createAndSaveFreeAgentPlayer(
                "nikita_test",
                "player_test"
        );

        TeamTransferResponse response =
                teamService.addPlayerToTeam(
                        team.getId(),
                        player.getId()
                );

        assertThat(response).isNotNull();
        assertThat(response.memberId())
                .isEqualTo(player.getId());
        assertThat(response.memberFullName())
                .isEqualTo("nikita_test player_test");
        assertThat(response.memberRole())
                .isEqualTo("PLAYER");
        assertThat(response.oldTeamName())
                .isEqualTo("Free Agent");
        assertThat(response.newTeamId())
                .isEqualTo(team.getId());
        assertThat(response.newTeamName())
                .isEqualTo("lakers_test");

        Player updatedPlayer =
                playerRepository.findPlayerWithTeamAndPositionsById(player.getId())
                        .orElseThrow();

        assertThat(updatedPlayer.getTeam())
                .isNotNull();

        assertThat(updatedPlayer.getTeam().getId())
                .isEqualTo(team.getId());

        Team updatedTeam =
                teamRepository.getTeamByIdOrThrow404(team.getId());

        assertThat(updatedTeam.getPlayers())
                .contains(player);
    }


    @Test
    void shouldTradePlayerFromOneTeamToAnotherTeam() {

        Team oldTeam = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team newTeam = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        Player player = createAndSavePlayer(
                "nikita_test",
                "player_test",
                oldTeam
        );

        TeamTransferResponse response =
                teamService.addPlayerToTeam(
                        newTeam.getId(),
                        player.getId()
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());
        assertThat(response.memberRole())
                .isEqualTo("PLAYER");
        assertThat(response.oldTeamName())
                .isEqualTo("lakers_test");
        assertThat(response.newTeamName())
                .isEqualTo("bulls_test");

        Player updatedPlayer =
                playerRepository.findPlayerWithTeamAndPositionsById(player.getId())
                        .orElseThrow();

        assertThat(updatedPlayer.getTeam().getId())
                .isEqualTo(newTeam.getId());

        Team updatedOldTeam =
                teamRepository.getTeamByIdOrThrow404(oldTeam.getId());

        Team updatedNewTeam =
                teamRepository.getTeamByIdOrThrow404(newTeam.getId());

        assertThat(updatedOldTeam.getPlayers())
                .doesNotContain(player);

        assertThat(updatedNewTeam.getPlayers())
                .contains(player);
    }


    @Test
    void shouldThrowExceptionWhenAddingPlayerToSameTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "nikita_test",
                "player_test",
                team
        );

        assertThatThrownBy(() ->
                teamService.addPlayerToTeam(
                        team.getId(),
                        player.getId()
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "Player with id " + player.getId()
                                + " already exists in team with id "
                                + team.getId()
                );
    }


    @Test
    void shouldThrowExceptionWhenAddingNonExistingPlayer() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Long nonExistingPlayerId = 999999L;

        assertThatThrownBy(() ->
                teamService.addPlayerToTeam(
                        team.getId(),
                        nonExistingPlayerId
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "Player with id " +
                                nonExistingPlayerId +
                                " not found"
                );
    }


    @Test
    void shouldThrowTeamNotFoundWhenAddingPlayerToNonExistingTeam() {

        Player player = createAndSaveFreeAgentPlayer(
                "nikita_test",
                "player_test"
        );

        Long nonExistingTeamId = 999999L;

        assertThatThrownBy(() ->
                teamService.addPlayerToTeam(
                        nonExistingTeamId,
                        player.getId()
                )
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    @Test
    void shouldThrowExceptionWhenAddingPlayerToFullTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        for (int i = 1; i <= 15; i++) {
            createAndSavePlayer(
                    "nikita_test",
                    "player" + i + "_test",
                    team
            );
        }

        Player newPlayer = createAndSaveFreeAgentPlayer(
                "nikita_test",
                "new_player_test"
        );

        assertThatThrownBy(() ->
                teamService.addPlayerToTeam(
                        team.getId(),
                        newPlayer.getId()
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "The team roster is full. Maximum 15 players allowed."
                );
    }


    // ============================================================
    // addCoachToTeam()
    // ============================================================

    @Test
    void shouldAddFreeAgentCoachToTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Coach coach = createAndSaveFreeAgentCoach(
                "sveta_test",
                "coach_test"
        );

        TeamTransferResponse response =
                teamService.addCoachToTeam(
                        team.getId(),
                        coach.getId()
                );

        assertThat(response).isNotNull();
        assertThat(response.memberId())
                .isEqualTo(coach.getId());
        assertThat(response.memberFullName())
                .isEqualTo("sveta_test coach_test");
        assertThat(response.memberRole())
                .isEqualTo("COACH");
        assertThat(response.oldTeamName())
                .isEqualTo("Free Agent");
        assertThat(response.newTeamId())
                .isEqualTo(team.getId());
        assertThat(response.newTeamName())
                .isEqualTo("lakers_test");

        Coach updatedCoach =
                coachRepository.findWithTeamById(coach.getId())
                        .orElseThrow();

        assertThat(updatedCoach.getTeam())
                .isNotNull();

        assertThat(updatedCoach.getTeam().getId())
                .isEqualTo(team.getId());

        Team updatedTeam =
                teamRepository.getTeamByIdOrThrow404(team.getId());

        assertThat(updatedTeam.getCoaches())
                .contains(coach);
    }


    @Test
    void shouldTradeCoachFromOneTeamToAnotherTeam() {

        Team oldTeam = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team newTeam = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        Coach coach = createAndSaveCoach(
                "sveta_test",
                "coach_test",
                oldTeam
        );

        TeamTransferResponse response =
                teamService.addCoachToTeam(
                        newTeam.getId(),
                        coach.getId()
                );

        assertThat(response.memberId())
                .isEqualTo(coach.getId());
        assertThat(response.memberRole())
                .isEqualTo("COACH");
        assertThat(response.oldTeamName())
                .isEqualTo("lakers_test");
        assertThat(response.newTeamName())
                .isEqualTo("bulls_test");

        Coach updatedCoach =
                coachRepository.findWithTeamById(coach.getId())
                        .orElseThrow();

        assertThat(updatedCoach.getTeam().getId())
                .isEqualTo(newTeam.getId());

        Team updatedOldTeam =
                teamRepository.getTeamByIdOrThrow404(oldTeam.getId());

        Team updatedNewTeam =
                teamRepository.getTeamByIdOrThrow404(newTeam.getId());

        assertThat(updatedOldTeam.getCoaches())
                .doesNotContain(coach);

        assertThat(updatedNewTeam.getCoaches())
                .contains(coach);
    }


    @Test
    void shouldThrowExceptionWhenAddingCoachToSameTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Coach coach = createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team
        );

        assertThatThrownBy(() ->
                teamService.addCoachToTeam(
                        team.getId(),
                        coach.getId()
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "Coach with id " + coach.getId()
                                + " already exists in team with id "
                                + team.getId()
                );
    }


    @Test
    void shouldThrowExceptionWhenAddingNonExistingCoach() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Long nonExistingCoachId = 999999L;

        assertThatThrownBy(() ->
                teamService.addCoachToTeam(
                        team.getId(),
                        nonExistingCoachId
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "Coach with id " +
                                nonExistingCoachId +
                                " not found"
                );
    }


    @Test
    void shouldThrowTeamNotFoundWhenAddingCoachToNonExistingTeam() {

        Coach coach = createAndSaveFreeAgentCoach(
                "sveta_test",
                "coach_test"
        );

        Long nonExistingTeamId = 999999L;

        assertThatThrownBy(() ->
                teamService.addCoachToTeam(
                        nonExistingTeamId,
                        coach.getId()
                )
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    @Test
    void shouldThrowExceptionWhenAddingCoachToFullTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        for (int i = 1; i <= 5; i++) {
            createAndSaveCoach(
                    "sveta_test",
                    "coach" + i + "_test",
                    team
            );
        }

        Coach newCoach = createAndSaveFreeAgentCoach(
                "sveta_test",
                "new_coach_test"
        );

        assertThatThrownBy(() ->
                teamService.addCoachToTeam(
                        team.getId(),
                        newCoach.getId()
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "The coaching staff is full. Maximum 5 coaches allowed."
                );
    }


    // ============================================================
    // deletePlayerFromTeam()
    // ============================================================

    @Test
    void shouldRemovePlayerFromTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Player player = createAndSavePlayer(
                "nikita_test",
                "player_test",
                team
        );

        TeamTransferResponse response =
                teamService.deletePlayerFromTeam(
                        team.getId(),
                        player.getId()
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());

        assertThat(response.memberRole())
                .isEqualTo("PLAYER");

        assertThat(response.oldTeamName())
                .isEqualTo("lakers_test");

        assertThat(response.newTeamName())
                .isEqualTo("Free Agent");

        Player updatedPlayer =
                playerRepository.findPlayerWithTeamAndPositionsById(player.getId())
                        .orElseThrow();

        assertThat(updatedPlayer.getTeam())
                .isNull();

        Team updatedTeam =
                teamRepository.getTeamByIdOrThrow404(team.getId());

        assertThat(updatedTeam.getPlayers())
                .doesNotContain(player);
    }


    @Test
    void shouldThrowExceptionWhenPlayerDoesNotBelongToTeam() {

        Team team1 = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team team2 = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        Player player = createAndSavePlayer(
                "nikita_test",
                "player_test",
                team1
        );

        assertThatThrownBy(() ->
                teamService.deletePlayerFromTeam(
                        team2.getId(),
                        player.getId()
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "Player with id " + player.getId()
                                + " doesn't exist in team with id "
                                + team2.getId()
                );
    }


    @Test
    void shouldThrowExceptionWhenRemovingNonExistingPlayer() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Long nonExistingPlayerId = 999999L;

        assertThatThrownBy(() ->
                teamService.deletePlayerFromTeam(
                        team.getId(),
                        nonExistingPlayerId
                )
        )
                .isInstanceOf(InvalidTeamDataException.class);
    }


    @Test
    void shouldThrowTeamNotFoundWhenRemovingPlayerFromNonExistingTeam() {

        Long nonExistingTeamId = 999999L;

        assertThatThrownBy(() ->
                teamService.deletePlayerFromTeam(
                        nonExistingTeamId,
                        999999L
                )
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    // ============================================================
    // deleteCoachFromTeam()
    // ============================================================

    @Test
    void shouldRemoveCoachFromTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Coach coach = createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team
        );

        TeamTransferResponse response =
                teamService.deleteCoachFromTeam(
                        team.getId(),
                        coach.getId()
                );

        assertThat(response.memberId())
                .isEqualTo(coach.getId());

        assertThat(response.memberRole())
                .isEqualTo("COACH");

        assertThat(response.oldTeamName())
                .isEqualTo("lakers_test");

        assertThat(response.newTeamName())
                .isEqualTo("Free Agent");

        Coach updatedCoach =
                coachRepository.findWithTeamById(coach.getId())
                        .orElseThrow();

        assertThat(updatedCoach.getTeam())
                .isNull();

        Team updatedTeam =
                teamRepository.getTeamByIdOrThrow404(team.getId());

        assertThat(updatedTeam.getCoaches())
                .doesNotContain(coach);
    }


    @Test
    void shouldThrowExceptionWhenCoachDoesNotBelongToTeam() {

        Team team1 = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Team team2 = createAndSaveTeam(
                "bulls_test",
                6,
                1966
        );

        Coach coach = createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team1
        );

        assertThatThrownBy(() ->
                teamService.deleteCoachFromTeam(
                        team2.getId(),
                        coach.getId()
                )
        )
                .isInstanceOf(InvalidTeamDataException.class)
                .hasMessage(
                        "Coach with id " + coach.getId()
                                + " doesn't exist in team with id "
                                + team2.getId()
                );
    }


    @Test
    void shouldThrowExceptionWhenRemovingNonExistingCoach() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Long nonExistingCoachId = 999999L;

        assertThatThrownBy(() ->
                teamService.deleteCoachFromTeam(
                        team.getId(),
                        nonExistingCoachId
                )
        )
                .isInstanceOf(InvalidTeamDataException.class);
    }


    @Test
    void shouldThrowTeamNotFoundWhenRemovingCoachFromNonExistingTeam() {

        Long nonExistingTeamId = 999999L;

        assertThatThrownBy(() ->
                teamService.deleteCoachFromTeam(
                        nonExistingTeamId,
                        999999L
                )
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    // ============================================================
    // fireAllTeamMembers()
    // ============================================================

    @Test
    void shouldFireAllTeamMembers() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Player player1 = createAndSavePlayer(
                "nikita_test",
                "player1_test",
                team
        );

        Player player2 = createAndSavePlayer(
                "nikita_test",
                "player2_test",
                team
        );

        Coach coach = createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team
        );

        teamService.fireAllTeamMembers(team.getId());

        Team updatedTeam =
                teamRepository.getTeamByIdOrThrow404(team.getId());

        assertThat(updatedTeam.getTeamMembers())
                .isEmpty();

        Player updatedPlayer1 =
                playerRepository.findPlayerWithTeamAndPositionsById(player1.getId())
                        .orElseThrow();

        Player updatedPlayer2 =
                playerRepository.findPlayerWithTeamAndPositionsById(player2.getId())
                        .orElseThrow();

        Coach updatedCoach =
                coachRepository.findWithTeamById(coach.getId())
                        .orElseThrow();

        assertThat(updatedPlayer1.getTeam()).isNull();
        assertThat(updatedPlayer2.getTeam()).isNull();
        assertThat(updatedCoach.getTeam()).isNull();
    }


    @Test
    void shouldDoNothingWhenFiringMembersFromEmptyTeam() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        assertThatCode(() ->
                teamService.fireAllTeamMembers(team.getId())
        )
                .doesNotThrowAnyException();

        Team updatedTeam =
                teamRepository.getTeamByIdOrThrow404(team.getId());

        assertThat(updatedTeam.getTeamMembers())
                .isEmpty();
    }


    @Test
    void shouldThrowTeamNotFoundWhenFiringMembersFromNonExistingTeam() {

        assertThatThrownBy(() ->
                teamService.fireAllTeamMembers(999999L)
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    // ============================================================
    // getTeamLineup()
    // ============================================================

    @Test
    void shouldReturnTeamLineup() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        Player player1 = createAndSavePlayer(
                "nikita_test",
                "player1_test",
                team
        );

        Player player2 = createAndSavePlayer(
                "nikita_test",
                "player2_test",
                team
        );

        createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team
        );

        TeamGroupResponse response =
                teamService.getTeamLineup(team.getId());

        assertThat(response).isNotNull();
        assertThat(response.teamName())
                .isEqualTo("lakers_test");
        assertThat(response.title())
                .isEqualTo(TeamGroupType.TEAM_LINEUP.name());

        assertThat(response.members())
                .hasSize(2);

        assertThat(response.members())
                .extracting(member -> member.fullName())
                .contains(
                        "nikita_test player1_test",
                        "nikita_test player2_test"
                );
    }


    @Test
    void shouldReturnEmptyTeamLineupWhenTeamHasNoPlayers() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        createAndSaveCoach(
                "sveta_test",
                "coach_test",
                team
        );

        TeamGroupResponse response =
                teamService.getTeamLineup(team.getId());

        assertThat(response.teamName())
                .isEqualTo("lakers_test");

        assertThat(response.title())
                .isEqualTo(TeamGroupType.TEAM_LINEUP.name());

        assertThat(response.members())
                .isEmpty();
    }


    @Test
    void shouldThrowTeamNotFoundWhenGettingLineupOfNonExistingTeam() {

        assertThatThrownBy(() ->
                teamService.getTeamLineup(999999L)
        )
                .isInstanceOf(TeamNotFoundException.class);
    }


    // ============================================================
    // getCoachingStaff()
    // ============================================================

    @Test
    void shouldReturnCoachingStaff() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        createAndSavePlayer(
                "nikita_test",
                "player_test",
                team
        );

        Coach coach1 = createAndSaveCoach(
                "sveta_test",
                "coach1_test",
                team
        );

        Coach coach2 = createAndSaveCoach(
                "sveta_test",
                "coach2_test",
                team
        );

        TeamGroupResponse response =
                teamService.getCoachingStaff(team.getId());

        assertThat(response).isNotNull();
        assertThat(response.teamName())
                .isEqualTo("lakers_test");

        assertThat(response.title())
                .isEqualTo(TeamGroupType.COACHING_STAFF.name());

        assertThat(response.members())
                .hasSize(2);

        assertThat(response.members())
                .extracting(member -> member.fullName())
                .contains(
                        "sveta_test coach1_test",
                        "sveta_test coach2_test"
                );
    }


    @Test
    void shouldReturnEmptyCoachingStaffWhenTeamHasNoCoaches() {

        Team team = createAndSaveTeam(
                "lakers_test",
                17,
                1947
        );

        createAndSavePlayer(
                "nikita_test",
                "player_test",
                team
        );

        TeamGroupResponse response =
                teamService.getCoachingStaff(team.getId());

        assertThat(response.teamName())
                .isEqualTo("lakers_test");

        assertThat(response.title())
                .isEqualTo(TeamGroupType.COACHING_STAFF.name());

        assertThat(response.members())
                .isEmpty();
    }


    @Test
    void shouldThrowTeamNotFoundWhenGettingCoachingStaffOfNonExistingTeam() {

        assertThatThrownBy(() ->
                teamService.getCoachingStaff(999999L)
        )
                .isInstanceOf(TeamNotFoundException.class);
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


    private Player createAndSaveFreeAgentPlayer(
            String firstName,
            String lastName
    ) {
        Player player = Player.builder()
                .firstName(firstName)
                .lastName(lastName)
                .salary(BigDecimal.valueOf(100000))
                .playerPositions(Set.of(PlayerPosition.PG))
                .rating(80)
                .championshipsWon(2)
                .build();

        return playerRepository.save(player);
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


    private Coach createAndSaveFreeAgentCoach(
            String firstName,
            String lastName
    ) {
        Coach coach = Coach.builder()
                .firstName(firstName)
                .lastName(lastName)
                .salary(BigDecimal.valueOf(150000))
                .yearsOfExperience(10)
                .championshipsWon(3)
                .build();

        return coachRepository.save(coach);
    }
}