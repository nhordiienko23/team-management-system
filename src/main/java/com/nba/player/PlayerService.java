package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import java.util.List;

public interface PlayerService {
    ResponsePlayerDto addPlayer(RequestPlayerDto dto);
    List<ResponsePlayerDto> getAllPlayers();
    ResponsePlayerDto updatePlayer(Long playerId, RequestPlayerDto dto);
    void deletePlayer(Long playerId);
    ResponsePlayerDto getPlayerById(Long playerId);
    List<ResponsePlayerDto> searchPlayers(PlayerSearchFilter filter);
    TeamGroupResponse getTeammatesByPlayerId(Long playerId);
}