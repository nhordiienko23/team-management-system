package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.exception.invalidData.InvalidPlayerDataException;
import com.nba.core.exception.notFound.PlayerNotFoundException;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
class PlayerServiceImpl implements PlayerService {
    private final PlayerMapper playerMapper;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    @Transactional
    @Override
    public ResponsePlayerDto addPlayer(RequestPlayerDto dto) {
        Player player = playerMapper.toPlayerEntity(dto);
        if (dto.teamId() != null) {
            player.setTeam(findTeamById(dto.teamId()));
        }
        Player savedPlayer = playerRepository.save(player);
        return playerMapper.toPlayerDto(savedPlayer);
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
    public ResponsePlayerDto updatePlayer(Long playerId, RequestPlayerDto dto) {
        Player player = playerRepository.getPlayerByIdOrThrow(playerId);

        player.setFirstName(dto.firstName());
        player.setLastName(dto.lastName());
        player.setSalary(dto.salary());
        if (dto.teamId() != null) {
            player.setTeam(findTeamById(dto.teamId()));
        } else {
            player.setTeam(null);
        }
        player.setPlayerPositions(dto.playerPositions());
        player.setRating(dto.rating());
        player.setChampionshipsWon(dto.championshipsWon());
        return playerMapper.toPlayerDto(player);
    }

    @Override
    @Transactional
    public void deletePlayer(Long playerId) {
        if (!playerRepository.existsById(playerId)) throw new PlayerNotFoundException(playerId);
        playerRepository.deleteById(playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponsePlayerDto getPlayerById(Long playerId) {
        return playerMapper.toPlayerDto(playerRepository.getPlayerByIdOrThrow(playerId));
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
    public TeamGroupResponse getTeammatesByPlayerId(Long playerId){
        Player player = playerRepository.getPlayerByIdOrThrow(playerId);
        if(player.getTeam() == null){
            throw new InvalidPlayerDataException("Player with id "+playerId+ " is a free agent and has no teammates");
        }
        Long teamId = player.getTeam().getId();
        return playerMapper.toResponseTeammates(playerRepository.findAllByTeamId(teamId), player);
    }



    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() ->
                new InvalidPlayerDataException("Team with id " + teamId + " not found"));
    }
}