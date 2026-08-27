package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;

import java.util.List;

public interface PlayerService {
    ResponsePlayerDto addPlayer(RequestPlayerDto dto);
    List<ResponsePlayerDto> getAllPlayers();
    ResponsePlayerDto partialUpdatePlayer(Long playerId, PatchPlayerRequest request);
    void deletePlayer(Long playerId);
    ResponsePlayerDto getPlayerById(Long playerId);
    List<ResponsePlayerDto> searchPlayers(PlayerSearchFilter filter);
    TeamGroupResponse getTeammatesByPlayerId(Long playerId);
    TeamTransferResponse changePlayerTeam(Long playerId, Long newTeamId);
}