package com.nba.player;

import java.util.List;

public interface PlayerService {
    ResponsePlayerDto addPlayer(RequestPlayerDto dto);

    List<ResponsePlayerDto> getAllPlayers();
    ResponsePlayerDto updatePlayer(Long id, RequestPlayerDto dto);
    void deletePlayer(Long id);
    ResponsePlayerDto getPlayerById(Long id);
    List<ResponsePlayerDto> searchPlayers(PlayerSearchFilter filter);
    ResponseTeammates getTeammatesByPlayerId(Long playerId);
}
