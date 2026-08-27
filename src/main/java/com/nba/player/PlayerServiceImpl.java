package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.core.exception.invalidData.InvalidPlayerDataException;
import com.nba.core.mapper.TeamTransferMapper;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
class PlayerServiceImpl implements PlayerService {
    private final PlayerMapper playerMapper;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TeamTransferMapper teamTransferMapper;

    @Transactional
    @Override
    public ResponsePlayerDto addPlayer(RequestPlayerDto dto) {
        Player player = playerMapper.toPlayerEntity(dto);
        if (dto.teamId() != null) {
            Team team = findTeamByIdOrThrow400(dto.teamId());
            team.addTeamMember(player);
        }
        return playerMapper.toPlayerDto(playerRepository.save(player));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePlayerDto> getAllPlayers() {
        return playerRepository.findAll().stream()
                .map(playerMapper::toPlayerDto)
                .toList();
    }

    @Override
    @Transactional
    public ResponsePlayerDto partialUpdatePlayer(Long playerId, PatchPlayerRequest request) {
        Player player = playerRepository.getPlayerByIdOrThrow404(playerId);

        if (request.firstName() != null) player.setFirstName(request.firstName());
        if (request.lastName() != null) player.setLastName(request.lastName());
        if (request.salary() != null) player.setSalary(request.salary());
        if (request.playerPositions() != null) player.setPlayerPositions(request.playerPositions());
        if (request.rating() != null) player.setRating(request.rating());
        if (request.championshipsWon() != null) player.setChampionshipsWon(request.championshipsWon());

        if (request.teamId() != null) {
            Team newTeam = findTeamByIdOrThrow400(request.teamId());
            if (player.getTeam() != null) {
                player.getTeam().removeTeamMember(player);
            }
            newTeam.addTeamMember(player);

        }

        return playerMapper.toPlayerDto(player);
    }

    @Override
    @Transactional
    public void deletePlayer(Long playerId) {
        Player player = playerRepository.getPlayerByIdOrThrow404(playerId);
        if (player.getTeam() != null) {
            player.getTeam().removeTeamMember(player);
        }
        playerRepository.delete(player);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponsePlayerDto getPlayerById(Long playerId) {
        return playerMapper.toPlayerDto(playerRepository.getPlayerByIdOrThrow404(playerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsePlayerDto> searchPlayers(PlayerSearchFilter filter) {
        return playerRepository.findAll(PlayerSpecification.buildQuery(filter)).stream()
                .map(playerMapper::toPlayerDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamGroupResponse getTeammatesByPlayerId(Long playerId) {
        Player player = playerRepository.getPlayerByIdOrThrow404(playerId);
        if (player.getTeam() == null) {
            throw new InvalidPlayerDataException("Player with id " + playerId + " is a free agent and has no teammates");
        }
        Long teamId = player.getTeam().getId();
        return playerMapper.toResponseTeammates(playerRepository.findAllByTeamId(teamId), player);
    }

    @Override
    @Transactional
    public TeamTransferResponse changePlayerTeam(Long playerId, Long newTeamId) {
        Player player = playerRepository.getPlayerByIdOrThrow404(playerId);
        Team oldTeam = player.getTeam();

        Long currentTeamId = player.getTeam() != null
                ? player.getTeam().getId()
                : null;

        if (Objects.equals(currentTeamId, newTeamId)) {
            throw new InvalidPlayerDataException(
                    newTeamId == null
                            ? "Player is already a free agent"
                            : "Player is already a team member of this team");
        }

        Team newTeam = null;

        if (newTeamId != null) {
            newTeam = findTeamByIdOrThrow400(newTeamId);
        }

        if (oldTeam != null) {
            oldTeam.removeTeamMember(player);
        }

        if (newTeam != null) {
            newTeam.addTeamMember(player);
        }
        return teamTransferMapper.toTransferResponse(player,
                oldTeam,
                newTeam,
                (newTeam != null)
                        ? "TRADE"
                        : "REMOVE");
    }

    private Team findTeamByIdOrThrow400(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() ->
                new InvalidPlayerDataException("Team with id " + teamId + " not found"));
    }


}