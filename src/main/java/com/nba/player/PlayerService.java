package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import java.util.List;

public interface PlayerService {
    ResponsePlayerDto addPlayer(RequestPlayerDto dto);
    List<ResponsePlayerDto> getAllPlayers();
    ResponsePlayerDto updatePlayer(Long id, RequestPlayerDto dto);
    void deletePlayer(Long id);
    ResponsePlayerDto getPlayerById(Long id);
    List<ResponsePlayerDto> searchPlayers(PlayerSearchFilter filter);
    TeamGroupResponse getTeammatesByPlayerId(Long playerId);
}