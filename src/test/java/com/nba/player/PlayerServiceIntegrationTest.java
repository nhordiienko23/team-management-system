package com.nba.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.core.exception.invalidData.InvalidPlayerDataException;
import com.nba.core.exception.notFound.PlayerNotFoundException;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlayerServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;


    // ============================================================
    // addPlayer
    // ============================================================

    @Test
    void shouldAddPlayerAsFreeAgent() {

        RequestPlayerDto request = new RequestPlayerDto(
                "LeBron",
                "James",
                BigDecimal.valueOf(50000000),
                null,
                Set.of(PlayerPosition.SF),
                95,
                4
        );

        ResponsePlayerDto response =
                playerService.addPlayer(request);

        assertThat(response.id())
                .isNotNull();

        assertThat(response.firstName())
                .isEqualTo("LeBron");

        assertThat(response.lastName())
                .isEqualTo("James");

        assertThat(response.team())
                .isNull();

        assertThat(response.rating())
                .isEqualTo(95);

        assertThat(response.championshipWon())
                .isEqualTo(4);

        assertThat(response.salary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(50000000)
                );

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(response.id())
                        .orElseThrow();

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("LeBron");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("James");

        assertThat(actualPlayer.getTeam())
                .isNull();

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactly(PlayerPosition.SF);

        assertThat(actualPlayer.getRating())
                .isEqualTo(95);

        assertThat(actualPlayer.getChampionshipsWon())
                .isEqualTo(4);

        assertThat(actualPlayer.getSalary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(50000000)
                );
    }


    @Test
    void shouldAddPlayerToTeam() {

        Team team = createAndSaveTeam(
                "service_add_player_team_test",
                5,
                1990
        );

        RequestPlayerDto request = new RequestPlayerDto(
                "Anthony",
                "Davis",
                BigDecimal.valueOf(35000000),
                team.getId(),
                Set.of(PlayerPosition.PF, PlayerPosition.C),
                90,
                2
        );

        ResponsePlayerDto response =
                playerService.addPlayer(request);

        assertThat(response.id())
                .isNotNull();

        assertThat(response.firstName())
                .isEqualTo("Anthony");

        assertThat(response.lastName())
                .isEqualTo("Davis");

        assertThat(response.team())
                .isEqualTo(team.getName());

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(response.id())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactlyInAnyOrder(
                        PlayerPosition.PF,
                        PlayerPosition.C
                );

        assertThat(
                playerRepository.findByIdAndTeamId(
                        response.id(),
                        team.getId()
                )
        )
                .isPresent();
    }


    @Test
    void shouldThrowExceptionWhenAddingPlayerToNonExistingTeam() {

        long playersBefore =
                playerRepository.count();

        RequestPlayerDto request = new RequestPlayerDto(
                "UniqueTest",
                "MissingTeamPlayer",
                BigDecimal.valueOf(50000000),
                999999L,
                Set.of(PlayerPosition.PG),
                96,
                4
        );

        assertThatThrownBy(() ->
                playerService.addPlayer(request)
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessage("Team with id 999999 not found");

        entityManager.clear();

        long playersAfter =
                playerRepository.count();

        assertThat(playersAfter)
                .isEqualTo(playersBefore);

        assertThat(playerRepository.findAll())
                .noneMatch(player ->
                        "UniqueTest".equals(player.getFirstName())
                                && "MissingTeamPlayer".equals(player.getLastName())
                );
    }


    @Test
    void shouldThrowExceptionWhenAddingPlayerToFullTeam() {

        Team team = createAndSaveTeam(
                "service_add_player_full_team_test",
                10,
                2000
        );

        for (int i = 1; i <= 15; i++) {
            createAndSavePlayer(
                    "Player",
                    "Roster" + i,
                    team
            );
        }

        long playersBefore =
                playerRepository.count();

        RequestPlayerDto request = new RequestPlayerDto(
                "Unique",
                "FullTeamPlayer",
                BigDecimal.valueOf(1000000),
                team.getId(),
                Set.of(PlayerPosition.PG),
                70,
                0
        );

        assertThatThrownBy(() ->
                playerService.addPlayer(request)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Maximum 15 players allowed"
                );

        entityManager.clear();

        long playersAfter =
                playerRepository.count();

        assertThat(playersAfter)
                .isEqualTo(playersBefore);

        assertThat(
                playerRepository.findAllByTeamId(team.getId())
        )
                .hasSize(15);

        assertThat(playerRepository.findAll())
                .noneMatch(player ->
                        "Unique".equals(player.getFirstName())
                                && "FullTeamPlayer".equals(player.getLastName())
                );
    }


    // ============================================================
    // getAllPlayers
    // ============================================================

    @Test
    void shouldGetAllPlayersWithPagination() {

        Team team = createAndSaveTeam(
                "service_all_players_team_test",
                5,
                1980
        );

        for (int i = 1; i <= 5; i++) {
            createAndSavePlayer(
                    "Pagination",
                    "Player" + i,
                    team
            );
        }

        entityManager.flush();
        entityManager.clear();

        Page<ResponsePlayerDto> result =
                playerService.getAllPlayers(
                        PageRequest.of(
                                0,
                                2,
                                Sort.by("id").ascending()
                        )
                );

        assertThat(result)
                .isNotNull();

        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getTotalElements())
                .isGreaterThanOrEqualTo(5);

        assertThat(result.getTotalPages())
                .isGreaterThanOrEqualTo(3);
    }


    @Test
    void shouldReturnEmptyPageWhenThereAreNoPlayersMatchingPage() {

        Page<ResponsePlayerDto> result =
                playerService.getAllPlayers(
                        PageRequest.of(100, 10)
                );

        assertThat(result.getContent())
                .isEmpty();
    }


    @Test
    void shouldReturnSinglePageWhenPageSizeIsGreaterThanTotalNumberOfPlayers() {

        entityManager.flush();
        entityManager.clear();

        Page<ResponsePlayerDto> result =
                playerService.getAllPlayers(
                        PageRequest.of(
                                0,
                                1000,
                                Sort.by("id").ascending()
                        )
                );

        assertThat(result)
                .isNotNull();

        assertThat(result.getTotalElements())
                .isGreaterThan(0);

        assertThat(result.getContent())
                .hasSize((int) result.getTotalElements());

        assertThat(result.getTotalPages())
                .isEqualTo(1);

        assertThat(result.isFirst())
                .isTrue();

        assertThat(result.isLast())
                .isTrue();
    }

    // ============================================================
    // getPlayerById
    // ============================================================

    @Test
    void shouldGetPlayerById() {

        Team team = createAndSaveTeam(
                "service_get_player_team_test",
                5,
                1995
        );

        Player player = createAndSavePlayer(
                "Nikola",
                "Jokic",
                team
        );

        entityManager.flush();
        entityManager.clear();

        ResponsePlayerDto response =
                playerService.getPlayerById(player.getId());

        assertThat(response.id())
                .isEqualTo(player.getId());

        assertThat(response.firstName())
                .isEqualTo("Nikola");

        assertThat(response.lastName())
                .isEqualTo("Jokic");

        assertThat(response.team())
                .isEqualTo(team.getName());

        assertThat(response.rating())
                .isEqualTo(90);
    }


    @Test
    void shouldThrowPlayerNotFoundExceptionWhenGettingMissingPlayer() {

        assertThatThrownBy(() ->
                playerService.getPlayerById(999999L)
        )
                .isInstanceOf(PlayerNotFoundException.class);
    }


    // ============================================================
    // partialUpdatePlayer
    // ============================================================

    @Test
    void shouldUpdateAllPlayerFields() {

        Team oldTeam = createAndSaveTeam(
                "service_update_old_team_test",
                5,
                1980
        );

        Team newTeam = createAndSaveTeam(
                "service_update_new_team_test",
                8,
                1990
        );

        Player player = createAndSavePlayer(
                "Old",
                "Player",
                oldTeam
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                "Updated",
                "Player",
                BigDecimal.valueOf(45000000),
                newTeam.getId(),
                Set.of(PlayerPosition.SG, PlayerPosition.SF),
                98,
                5
        );

        ResponsePlayerDto response =
                playerService.partialUpdatePlayer(
                        player.getId(),
                        request
                );

        assertThat(response.firstName())
                .isEqualTo("Updated");

        assertThat(response.lastName())
                .isEqualTo("Player");

        assertThat(response.salary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(45000000)
                );

        assertThat(response.rating())
                .isEqualTo(98);

        assertThat(response.championshipWon())
                .isEqualTo(5);

        assertThat(response.team())
                .isEqualTo(newTeam.getName());

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("Updated");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("Player");

        assertThat(actualPlayer.getSalary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(45000000)
                );

        assertThat(actualPlayer.getRating())
                .isEqualTo(98);

        assertThat(actualPlayer.getChampionshipsWon())
                .isEqualTo(5);

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(newTeam.getId());

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactlyInAnyOrder(
                        PlayerPosition.SG,
                        PlayerPosition.SF
                );

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        oldTeam.getId()
                )
        )
                .isEmpty();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        newTeam.getId()
                )
        )
                .isPresent();
    }


    @Test
    void shouldMoveFreeAgentToTeamUsingPatch() {

        Team team = createAndSaveTeam(
                "service_patch_free_agent_team_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Free",
                "Agent",
                null
        );

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        null,
                        null,
                        null,
                        team.getId(),
                        null,
                        null,
                        null
                );

        ResponsePlayerDto response =
                playerService.partialUpdatePlayer(
                        player.getId(),
                        request
                );

        assertThat(response.team())
                .isEqualTo(team.getName());

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        team.getId()
                )
        )
                .isPresent();
    }


    @Test
    void shouldUpdateOnlyProvidedPlayerFields() {

        Team team = createAndSaveTeam(
                "service_partial_player_update_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "LeBron",
                "James",
                team
        );

        PatchPlayerRequest request = new PatchPlayerRequest(
                "Updated",
                null,
                null,
                null,
                null,
                null,
                null
        );

        ResponsePlayerDto response =
                playerService.partialUpdatePlayer(
                        player.getId(),
                        request
                );

        assertThat(response.firstName())
                .isEqualTo("Updated");

        assertThat(response.lastName())
                .isEqualTo("James");

        assertThat(response.team())
                .isEqualTo(team.getName());

        assertThat(response.salary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(35000000)
                );

        assertThat(response.rating())
                .isEqualTo(90);

        assertThat(response.championshipWon())
                .isEqualTo(2);
    }


    @Test
    void shouldUpdateOnlyPlayerPositions() {

        Team team = createAndSaveTeam(
                "service_patch_positions_only_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Positions",
                "Only",
                team
        );

        Long playerId = player.getId();

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        null,
                        null,
                        null,
                        null,
                        Set.of(
                                PlayerPosition.SG,
                                PlayerPosition.SF
                        ),
                        null,
                        null
                );

        ResponsePlayerDto response =
                playerService.partialUpdatePlayer(
                        playerId,
                        request
                );

        assertThat(response.firstName())
                .isEqualTo("Positions");

        assertThat(response.lastName())
                .isEqualTo("Only");

        assertThat(response.salary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(35000000)
                );

        assertThat(response.rating())
                .isEqualTo(90);

        assertThat(response.championshipWon())
                .isEqualTo(2);

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(playerId)
                        .orElseThrow();

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("Positions");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("Only");

        assertThat(actualPlayer.getSalary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(35000000)
                );

        assertThat(actualPlayer.getRating())
                .isEqualTo(90);

        assertThat(actualPlayer.getChampionshipsWon())
                .isEqualTo(2);

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactlyInAnyOrder(
                        PlayerPosition.SG,
                        PlayerPosition.SF
                );

        assertThat(
                playerRepository.findByIdAndTeamId(
                        playerId,
                        team.getId()
                )
        )
                .isPresent();
    }


    @Test
    void shouldUpdateOnlySalaryRatingAndChampionshipsWon() {

        Team team = createAndSaveTeam(
                "service_patch_numeric_fields_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Numeric",
                "Fields",
                team
        );

        Long playerId = player.getId();

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        null,
                        null,
                        BigDecimal.valueOf(45000000),
                        null,
                        null,
                        97,
                        6
                );

        ResponsePlayerDto response =
                playerService.partialUpdatePlayer(
                        playerId,
                        request
                );

        assertThat(response.firstName())
                .isEqualTo("Numeric");

        assertThat(response.lastName())
                .isEqualTo("Fields");

        assertThat(response.salary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(45000000)
                );

        assertThat(response.rating())
                .isEqualTo(97);

        assertThat(response.championshipWon())
                .isEqualTo(6);

        assertThat(response.team())
                .isEqualTo(team.getName());

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(playerId)
                        .orElseThrow();

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("Numeric");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("Fields");

        assertThat(actualPlayer.getSalary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(45000000)
                );

        assertThat(actualPlayer.getRating())
                .isEqualTo(97);

        assertThat(actualPlayer.getChampionshipsWon())
                .isEqualTo(6);

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(team.getId());

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactly(PlayerPosition.PG);
    }


    @Test
    void shouldAllowPatchWithNoFields() {

        Team team = createAndSaveTeam(
                "service_empty_patch_test",
                7,
                1990
        );

        Player player = createAndSavePlayer(
                "Luka",
                "Doncic",
                team
        );

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        ResponsePlayerDto response =
                playerService.partialUpdatePlayer(
                        player.getId(),
                        request
                );

        assertThat(response.firstName())
                .isEqualTo("Luka");

        assertThat(response.lastName())
                .isEqualTo("Doncic");

        assertThat(response.team())
                .isEqualTo(team.getName());

        assertThat(response.rating())
                .isEqualTo(90);

        assertThat(response.championshipWon())
                .isEqualTo(2);
    }


    @Test
    void shouldThrowExceptionWhenUpdatingMissingPlayer() {

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        "Updated",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertThatThrownBy(() ->
                playerService.partialUpdatePlayer(
                        999999L,
                        request
                )
        )
                .isInstanceOf(PlayerNotFoundException.class);
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackPlayerUpdateWhenTargetTeamDoesNotExist() {

        Team team = createAndSaveTeam(
                "service_patch_rollback_team_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Original",
                "Player",
                team
        );

        Long playerId = player.getId();
        Long teamId = team.getId();

        entityManager.clear();

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        "Changed",
                        "Name",
                        BigDecimal.valueOf(99999999),
                        999999L,
                        Set.of(PlayerPosition.C),
                        99,
                        9
                );

        assertThatThrownBy(() ->
                playerService.partialUpdatePlayer(
                        playerId,
                        request
                )
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessageContaining(
                        "Team with id 999999 not found"
                );

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(playerId)
                        .orElseThrow();

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("Original");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("Player");

        assertThat(actualPlayer.getSalary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(35000000)
                );

        assertThat(actualPlayer.getRating())
                .isEqualTo(90);

        assertThat(actualPlayer.getChampionshipsWon())
                .isEqualTo(2);

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactly(PlayerPosition.PG);

        assertThat(
                playerRepository.findByIdAndTeamId(
                        playerId,
                        teamId
                )
        )
                .isPresent();
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackPlayerTransferWhenPatchTargetTeamIsFull() {

        Team oldTeam = createAndSaveTeam(
                "service_patch_old_team_full_test",
                5,
                1990
        );

        Team newTeam = createAndSaveTeam(
                "service_patch_new_team_full_test",
                8,
                2000
        );

        Player player = createAndSavePlayer(
                "Original",
                "Player",
                oldTeam
        );

        for (int i = 1; i <= 15; i++) {
            createAndSavePlayer(
                    "Full",
                    "Patch" + i,
                    newTeam
            );
        }

        entityManager.clear();

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        "Changed",
                        null,
                        BigDecimal.valueOf(99999999),
                        newTeam.getId(),
                        Set.of(PlayerPosition.C),
                        99,
                        9
                );

        assertThatThrownBy(() ->
                playerService.partialUpdatePlayer(
                        player.getId(),
                        request
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Maximum 15 players allowed"
                );

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getFirstName())
                .isEqualTo("Original");

        assertThat(actualPlayer.getLastName())
                .isEqualTo("Player");

        assertThat(actualPlayer.getSalary())
                .isEqualByComparingTo(
                        BigDecimal.valueOf(35000000)
                );

        assertThat(actualPlayer.getRating())
                .isEqualTo(90);

        assertThat(actualPlayer.getChampionshipsWon())
                .isEqualTo(2);

        assertThat(actualPlayer.getPlayerPositions())
                .containsExactly(PlayerPosition.PG);

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        oldTeam.getId()
                )
        )
                .isPresent();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        newTeam.getId()
                )
        )
                .isEmpty();

        assertThat(
                playerRepository.findAllByTeamId(
                        newTeam.getId()
                )
        )
                .hasSize(15);
    }


    @Test
    void shouldThrowExceptionWhenUpdatingPlayerToNonExistingTeam() {

        Player player = createAndSavePlayer(
                "Free",
                "Agent",
                null
        );

        PatchPlayerRequest request =
                new PatchPlayerRequest(
                        null,
                        null,
                        null,
                        999999L,
                        null,
                        null,
                        null
                );

        assertThatThrownBy(() ->
                playerService.partialUpdatePlayer(
                        player.getId(),
                        request
                )
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessageContaining(
                        "Team with id 999999 not found"
                );

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();
    }


    // ============================================================
    // deletePlayer
    // ============================================================

    @Test
    void shouldDeletePlayerFromTeam() {

        Team team = createAndSaveTeam(
                "service_delete_player_team_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                team
        );

        Long playerId = player.getId();

        playerService.deletePlayer(playerId);

        entityManager.flush();
        entityManager.clear();

        assertThat(playerRepository.findById(playerId))
                .isEmpty();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        playerId,
                        team.getId()
                )
        )
                .isEmpty();
    }


    @Test
    void shouldDeleteFreeAgentPlayer() {

        Player player = createAndSavePlayer(
                "Free",
                "Agent",
                null
        );

        Long playerId = player.getId();

        playerService.deletePlayer(playerId);

        entityManager.flush();
        entityManager.clear();

        assertThat(playerRepository.findById(playerId))
                .isEmpty();
    }


    @Test
    void shouldThrowExceptionWhenDeletingMissingPlayer() {

        assertThatThrownBy(() ->
                playerService.deletePlayer(999999L)
        )
                .isInstanceOf(PlayerNotFoundException.class);
    }


    // ============================================================
    // searchPlayers
    // ============================================================

    @Test
    void shouldSearchPlayersByAllFilters() {

        Team lakers = createAndSaveTeam(
                "Service Lakers Search",
                10,
                1947
        );

        Team bulls = createAndSaveTeam(
                "Service Bulls Search",
                6,
                1966
        );

        Player matchingPlayer = Player.builder()
                .firstName("LeBron")
                .lastName("James")
                .salary(BigDecimal.valueOf(50000000))
                .team(lakers)
                .playerPositions(
                        Set.of(
                                PlayerPosition.SF,
                                PlayerPosition.PF
                        )
                )
                .rating(95)
                .championshipsWon(4)
                .build();

        playerRepository.save(matchingPlayer);
        lakers.addTeamMember(matchingPlayer);

        Player wrongPlayer = Player.builder()
                .firstName("Stephen")
                .lastName("Curry")
                .salary(BigDecimal.valueOf(55000000))
                .team(bulls)
                .playerPositions(Set.of(PlayerPosition.PG))
                .rating(98)
                .championshipsWon(3)
                .build();

        playerRepository.save(wrongPlayer);
        bulls.addTeamMember(wrongPlayer);

        entityManager.flush();
        entityManager.clear();

        PlayerSearchFilter filter =
                new PlayerSearchFilter(
                        "Leb",
                        "Jam",
                        "Lakers",
                        90,
                        100,
                        3,
                        5,
                        List.of("PG", "SF"),
                        BigDecimal.valueOf(40000000),
                        BigDecimal.valueOf(60000000)
                );

        Page<ResponsePlayerDto> result =
                playerService.searchPlayers(
                        filter,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .hasSize(1);

        ResponsePlayerDto response =
                result.getContent().get(0);

        assertThat(response.firstName())
                .isEqualTo("LeBron");

        assertThat(response.lastName())
                .isEqualTo("James");

        assertThat(response.team())
                .isEqualTo("Service Lakers Search");

        assertThat(response.rating())
                .isEqualTo(95);

        assertThat(response.championshipWon())
                .isEqualTo(4);
    }


    @Test
    void shouldReturnAllPlayersWhenSearchFilterIsEmpty() {

        PlayerSearchFilter filter =
                new PlayerSearchFilter(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponsePlayerDto> result =
                playerService.searchPlayers(
                        filter,
                        PageRequest.of(0, 10)
                );

        assertThat(result)
                .isNotNull();

        assertThat(result.getTotalElements())
                .isGreaterThan(0);
    }


    @Test
    void shouldSearchPlayersByNameCaseInsensitively() {

        Team team = createAndSaveTeam(
                "Service Search Name Team",
                3,
                2000
        );

        createAndSavePlayer(
                "LeBron",
                "James",
                team
        );

        entityManager.flush();
        entityManager.clear();

        PlayerSearchFilter filter =
                new PlayerSearchFilter(
                        "LEBRON",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponsePlayerDto> result =
                playerService.searchPlayers(
                        filter,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .isNotEmpty();

        assertThat(result.getContent())
                .anyMatch(player ->
                        "LeBron".equals(player.firstName())
                );
    }


    @Test
    void shouldSearchPlayersByTeamName() {

        Team team = createAndSaveTeam(
                "Service Golden State Search",
                7,
                1946
        );

        createAndSavePlayer(
                "UniqueStephen",
                "UniqueCurry",
                team
        );

        entityManager.flush();
        entityManager.clear();

        PlayerSearchFilter filter =
                new PlayerSearchFilter(
                        null,
                        null,
                        "golden state",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponsePlayerDto> result =
                playerService.searchPlayers(
                        filter,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .isNotEmpty();

        assertThat(result.getContent())
                .anyMatch(player ->
                        "UniqueStephen".equals(player.firstName())
                );
    }


    @Test
    void shouldSearchPlayersUsingRatingBoundaries() {

        Team team = createAndSaveTeam(
                "service_rating_boundary_team_test",
                5,
                1990
        );

        Player player =
                Player.builder()
                        .firstName("Boundary")
                        .lastName("Rating")
                        .salary(BigDecimal.valueOf(1000000))
                        .team(team)
                        .playerPositions(Set.of(PlayerPosition.PG))
                        .rating(90)
                        .championshipsWon(2)
                        .build();

        playerRepository.save(player);
        team.addTeamMember(player);

        entityManager.flush();
        entityManager.clear();

        PlayerSearchFilter filter =
                new PlayerSearchFilter(
                        "Boundary",
                        null,
                        null,
                        90,
                        90,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponsePlayerDto> result =
                playerService.searchPlayers(
                        filter,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .hasSize(1);

        assertThat(result.getContent().get(0).rating())
                .isEqualTo(90);
    }


    @Test
    void shouldReturnEmptyResultWhenSearchDoesNotMatch() {

        PlayerSearchFilter filter =
                new PlayerSearchFilter(
                        "Nobody",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Page<ResponsePlayerDto> result =
                playerService.searchPlayers(
                        filter,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .isEmpty();
    }


    @Test
    void shouldReturnEmptyResultWhenPositionFilterIsUnknown() {

        PlayerSearchFilter filter =
                new PlayerSearchFilter(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("UNKNOWN_POSITION"),
                        null,
                        null
                );

        Page<ResponsePlayerDto> result =
                playerService.searchPlayers(
                        filter,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent())
                .isEmpty();
    }


    // ============================================================
    // getTeammatesByPlayerId
    // ============================================================

    @Test
    void shouldGetPlayerTeammates() {

        Team team = createAndSaveTeam(
                "service_teammates_team_test",
                5,
                1990
        );

        Team anotherTeam = createAndSaveTeam(
                "service_another_team_test",
                5,
                1991
        );

        Player firstPlayer = createAndSavePlayer(
                "First",
                "Player",
                team
        );

        createAndSavePlayer(
                "Second",
                "Player",
                team
        );

        createAndSavePlayer(
                "Third",
                "Player",
                team
        );

        createAndSavePlayer(
                "Other",
                "TeamPlayer",
                anotherTeam
        );

        entityManager.flush();
        entityManager.clear();

        TeamGroupResponse response =
                playerService.getTeammatesByPlayerId(
                        firstPlayer.getId()
                );

        assertThat(response)
                .isNotNull();

        String responseJson =
                writeAsJson(response);

        assertThat(responseJson)
                .contains("Second");

        assertThat(responseJson)
                .contains("Third");

        assertThat(responseJson)
                .doesNotContain("Other");

        assertThat(responseJson)
                .doesNotContain("\"firstName\":\"First\"");
    }


    @Test
    void shouldThrowExceptionWhenFreeAgentRequestsTeammates() {

        Player freeAgent = createAndSavePlayer(
                "Free",
                "Agent",
                null
        );

        assertThatThrownBy(() ->
                playerService.getTeammatesByPlayerId(
                        freeAgent.getId()
                )
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessageContaining(
                        "is a free agent and has no teammates"
                );
    }


    @Test
    void shouldThrowExceptionWhenMissingPlayerRequestsTeammates() {

        assertThatThrownBy(() ->
                playerService.getTeammatesByPlayerId(999999L)
        )
                .isInstanceOf(PlayerNotFoundException.class);
    }


    // ============================================================
    // changePlayerTeam
    // ============================================================

    @Test
    void shouldAddFreeAgentPlayerToNewTeam() {

        Team newTeam = createAndSaveTeam(
                "service_change_free_agent_team_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Luka",
                "Doncic",
                null
        );

        TeamTransferResponse response =
                playerService.changePlayerTeam(
                        player.getId(),
                        newTeam.getId()
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());

        assertThat(response.oldTeamName())
                .isEqualTo("Free Agent");

        assertThat(response.newTeamName())
                .isEqualTo(newTeam.getName());

        assertThat(response.newTeamId())
                .isEqualTo(newTeam.getId());

        entityManager.flush();
        entityManager.clear();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        newTeam.getId()
                )
        )
                .isPresent();
    }


    @Test
    void shouldTradePlayerToAnotherTeam() {

        Team oldTeam = createAndSaveTeam(
                "service_change_old_team_test",
                5,
                1990
        );

        Team newTeam = createAndSaveTeam(
                "service_change_new_team_test",
                8,
                2000
        );

        Player player = createAndSavePlayer(
                "LeBron",
                "James",
                oldTeam
        );

        TeamTransferResponse response =
                playerService.changePlayerTeam(
                        player.getId(),
                        newTeam.getId()
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());

        assertThat(response.oldTeamName())
                .isEqualTo(oldTeam.getName());

        assertThat(response.newTeamName())
                .isEqualTo(newTeam.getName());

        assertThat(response.newTeamId())
                .isEqualTo(newTeam.getId());

        entityManager.flush();
        entityManager.clear();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        oldTeam.getId()
                )
        )
                .isEmpty();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        newTeam.getId()
                )
        )
                .isPresent();
    }


    @Test
    void shouldRemovePlayerFromTeamAndMakeFreeAgent() {

        Team oldTeam = createAndSaveTeam(
                "service_remove_player_team_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Anthony",
                "Davis",
                oldTeam
        );

        TeamTransferResponse response =
                playerService.changePlayerTeam(
                        player.getId(),
                        null
                );

        assertThat(response.memberId())
                .isEqualTo(player.getId());

        assertThat(response.oldTeamName())
                .isEqualTo(oldTeam.getName());

        assertThat(response.newTeamName())
                .isEqualTo("Free Agent");

        assertThat(response.newTeamId())
                .isNull();

        entityManager.flush();
        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        oldTeam.getId()
                )
        )
                .isEmpty();
    }


    @Test
    void shouldThrowExceptionWhenPlayerIsAlreadyFreeAgent() {

        Player player = createAndSavePlayer(
                "Free",
                "Agent",
                null
        );

        assertThatThrownBy(() ->
                playerService.changePlayerTeam(
                        player.getId(),
                        null
                )
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessage("Player is already a free agent");
    }


    @Test
    void shouldThrowExceptionWhenPlayerIsAlreadyInRequestedTeam() {

        Team team = createAndSaveTeam(
                "service_same_team_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "LeBron",
                "James",
                team
        );

        assertThatThrownBy(() ->
                playerService.changePlayerTeam(
                        player.getId(),
                        team.getId()
                )
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessage(
                        "Player is already a team member of this team"
                );

        entityManager.clear();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        team.getId()
                )
        )
                .isPresent();
    }


    @Test
    void shouldThrowExceptionWhenChangingPlayerToNonExistingTeam() {

        Player player = createAndSavePlayer(
                "Luka",
                "Doncic",
                null
        );

        assertThatThrownBy(() ->
                playerService.changePlayerTeam(
                        player.getId(),
                        999999L
                )
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessageContaining(
                        "Team with id 999999 not found"
                );

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNull();
    }


    @Test
    void shouldNotChangePlayerTeamWhenTargetTeamDoesNotExist() {

        Team oldTeam = createAndSaveTeam(
                "service_non_existing_target_old_team_test",
                5,
                1990
        );

        Player player = createAndSavePlayer(
                "Old",
                "TeamPlayer",
                oldTeam
        );

        assertThatThrownBy(() ->
                playerService.changePlayerTeam(
                        player.getId(),
                        999999L
                )
        )
                .isInstanceOf(InvalidPlayerDataException.class)
                .hasMessageContaining(
                        "Team with id 999999 not found"
                );

        entityManager.clear();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        oldTeam.getId()
                )
        )
                .isPresent();
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackPlayerTransferWhenNewTeamIsFull() {

        Team oldTeam = createAndSaveTeam(
                "service_change_old_full_test",
                5,
                1990
        );

        Team newTeam = createAndSaveTeam(
                "service_change_new_full_test",
                8,
                2000
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

        entityManager.clear();

        assertThatThrownBy(() ->
                playerService.changePlayerTeam(
                        player.getId(),
                        newTeam.getId()
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Maximum 15 players allowed"
                );

        entityManager.clear();

        Player actualPlayer =
                playerRepository.findById(player.getId())
                        .orElseThrow();

        assertThat(actualPlayer.getTeam())
                .isNotNull();

        assertThat(actualPlayer.getTeam().getId())
                .isEqualTo(oldTeam.getId());

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        oldTeam.getId()
                )
        )
                .isPresent();

        assertThat(
                playerRepository.findByIdAndTeamId(
                        player.getId(),
                        newTeam.getId()
                )
        )
                .isEmpty();

        assertThat(
                playerRepository.findAllByTeamId(
                        newTeam.getId()
                )
        )
                .hasSize(15);
    }


    @Test
    void shouldThrowExceptionWhenChangingMissingPlayerTeam() {

        assertThatThrownBy(() ->
                playerService.changePlayerTeam(
                        999999L,
                        null
                )
        )
                .isInstanceOf(PlayerNotFoundException.class);
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


    private String writeAsJson(Object object) {

        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
